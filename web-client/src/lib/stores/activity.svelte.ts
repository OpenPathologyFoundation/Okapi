import { browser } from '$app/environment';
import { goto } from '$app/navigation';
import { ACTIVITY_REGISTRY } from '$lib/activity-registry';
import { csrfHeaders } from '$lib/csrf';
import type {
	ActivityContext,
	ActivityDefinition,
	ActivityInstance,
	ReportActivityContext,
	ViewerActivityContext,
} from '$lib/types/activity';
import { ReportBridge } from '$lib/report-bridge';
import { ViewerBridge } from '$lib/viewer-bridge';
import type { ReportLaunchConfig } from '$lib/types/report-bridge';
import type { ViewerLaunchConfig, ViewerMode } from '$lib/types/viewer-bridge';

interface TokenResponse {
	accessToken: string;
	tokenType: string;
	expiresInSeconds: number;
	okapiAuthzVersion: string;
}

function createEmptyActivity(activityId: string): ActivityInstance {
	return {
		activityId,
		status: 'idle',
		caseId: null,
		accession: null,
		bridge: null,
		moduleState: {},
		lastError: null,
		visible: false,
		launchConfig: undefined,
	};
}

class ActivityStore {
	activities = $state<Record<string, ActivityInstance>>({});

	getActivity(activityId: string): ActivityInstance | null {
		return this.activities[activityId] ?? null;
	}

	register(activityId: string, activity: ActivityInstance): void {
		this.activities = {
			...this.activities,
			[activityId]: activity,
		};
	}

	update(activityId: string, patch: Partial<ActivityInstance>): void {
		const current = this.getActivity(activityId) ?? createEmptyActivity(activityId);
		this.activities = {
			...this.activities,
			[activityId]: {
				...current,
				...patch,
				moduleState: patch.moduleState ?? current.moduleState,
			},
		};
	}

	async launchActivity(activityId: string, context: ActivityContext): Promise<void> {
		if (!browser) return;

		const definition = ACTIVITY_REGISTRY[activityId];
		if (!definition) {
			throw new Error(`Unknown activity: ${activityId}`);
		}

		if (definition.type === 'internal') {
			await goto(definition.caseScoped ? `${definition.path}/${context.accession}` : definition.path);
			return;
		}

		switch (definition.bridge) {
			case 'viewer':
				await this.launchViewerActivity(definition, context as ViewerActivityContext);
				return;
			case 'report':
				await this.launchReportActivity(definition, context as ReportActivityContext);
				return;
			default:
				throw new Error(`Unsupported bridge for activity: ${activityId}`);
		}
	}

	focusActivity(activityId: string, focused: boolean): void {
		const activity = this.getActivity(activityId);
		if (!activity?.bridge || activity.status !== 'connected') return;
		activity.bridge.sendFocusState(focused ? 'active' : 'blurred');
	}

	setFocus(activityId: string | null): void {
		for (const [id, activity] of Object.entries(this.activities)) {
			if (!activity.bridge || activity.status !== 'connected') continue;
			activity.bridge.sendFocusState(activityId === id ? 'active' : 'blurred');
		}
	}

	sendCaseChange(activityId: string, caseId: string, accession: string): void {
		const activity = this.getActivity(activityId);
		if (!activity?.bridge || activity.status !== 'connected') return;
		if (activityId === 'pelican-viewer') {
			(activity.bridge as ViewerBridge).sendCaseChange(caseId, accession);
			this.update(activityId, {
				caseId,
				accession,
				moduleState: { ...activity.moduleState, slideCount: undefined },
				lastError: null,
			});
			return;
		}
		activity.bridge.sendContextUpdate('case', { caseId, accession });
		this.update(activityId, {
			caseId,
			accession,
			lastError: null,
		});
	}

	attachIframeTarget(activityId: string, targetWindow: Window | null): void {
		const activity = this.getActivity(activityId);
		if (!activity?.bridge || !targetWindow || !activity.launchConfig) return;
		if (activity.status === 'connected') return;

		activity.bridge.attachToTarget(activity.launchConfig as any, targetWindow, {
			ownsTargetWindow: false,
			trackWindowClosed: false,
		});
	}

	closeActivity(activityId: string): void {
		const activity = this.getActivity(activityId);
		if (!activity) return;
		activity.bridge?.close();
		this.update(activityId, {
			status: 'closed',
			caseId: null,
			accession: null,
			moduleState: {},
			lastError: null,
			visible: false,
			launchConfig: undefined,
		});
	}

	destroyAll(): void {
		for (const [activityId, activity] of Object.entries(this.activities)) {
			activity.bridge?.destroy();
			this.update(activityId, {
				status: 'closed',
				caseId: null,
				accession: null,
				bridge: null,
				moduleState: {},
				lastError: null,
				visible: false,
				launchConfig: undefined,
			});
		}
	}

	private async launchViewerActivity(definition: ActivityDefinition, context: ViewerActivityContext): Promise<void> {
		const activityId = definition.id;
		const existing = this.getActivity(activityId);
		const requestedMode = context.mode ?? 'clinical';

		if (existing?.bridge && existing.status === 'connected') {
			const currentMode = (existing.moduleState.mode as ViewerActivityContext['mode']) ?? 'clinical';
			if (currentMode !== requestedMode) {
				existing.bridge.close();
				existing.bridge.destroy();
				this.register(activityId, createEmptyActivity(activityId));
			} else if (existing.caseId === context.caseId) {
				this.setFocus(activityId);
				return;
			} else {
				(existing.bridge as ViewerBridge).sendCaseChange(context.caseId, context.accession);
				this.update(activityId, {
					caseId: context.caseId,
					accession: context.accession,
					moduleState: {
						...existing.moduleState,
						mode: requestedMode,
						slideCount: undefined,
					},
					lastError: null,
					visible: true,
				});
				return;
			}
		}

		if (existing?.status === 'launching') return;

		const token = await this.mintToken();
		if (!token) {
			this.update(activityId, {
				lastError: { code: 'TOKEN_MINT_FAILED', message: 'Failed to obtain viewer token' },
			});
			return;
		}

		const bridge = new ViewerBridge({
			mintToken: () => this.mintToken(),
		});

		bridge.on('stateChange', (status) => {
			this.update(activityId, { status });
		});
		bridge.on('caseLoaded', (data) => {
			const current = this.getActivity(activityId) ?? createEmptyActivity(activityId);
			this.update(activityId, {
				moduleState: {
					...current.moduleState,
					slideCount: data.slideCount,
				},
			});
		});
		bridge.on('viewerError', (lastError) => {
			this.update(activityId, { lastError });
		});

		this.register(activityId, {
			activityId,
			status: 'idle',
			caseId: context.caseId,
			accession: context.accession,
			bridge,
			moduleState: {
				mode: requestedMode,
				slideCount: undefined,
			},
			lastError: null,
			visible: true,
			launchConfig: undefined,
		});

		bridge.launch({
			token,
			caseId: context.caseId,
			accession: context.accession,
			viewerUrl: definition.path,
			viewerOrigin: window.location.origin,
			tileServerUrl: context.tileServerUrl,
			sessionServiceUrl: context.sessionServiceUrl,
			userId: context.userId,
			mode: requestedMode,
		});
	}

	private async launchReportActivity(definition: ActivityDefinition, context: ReportActivityContext): Promise<void> {
		const activityId = definition.id;
		const existing = this.getActivity(activityId);

		if (existing?.bridge && existing.status === 'connected') {
			if (existing.caseId === context.caseId) {
				this.update(activityId, { visible: true });
				return;
			}

			existing.bridge.sendContextUpdate('case', {
				caseId: context.caseId,
				accession: context.accession,
				role: context.role,
			});
			this.update(activityId, {
				caseId: context.caseId,
				accession: context.accession,
				moduleState: {
					...existing.moduleState,
					isDirty: false,
				},
				lastError: null,
				visible: true,
			});
			return;
		}

		const token = await this.mintToken();
		if (!token) {
			this.update(activityId, {
				lastError: { code: 'TOKEN_MINT_FAILED', message: 'Failed to obtain report token' },
			});
			return;
		}

		const bridge = new ReportBridge();
		bridge.on('stateChange', (status) => {
			this.update(activityId, { status });
		});
		bridge.on('stateUpdate', (data) => {
			const current = this.getActivity(activityId) ?? createEmptyActivity(activityId);
			this.update(activityId, {
				moduleState: {
					...current.moduleState,
					[data.key]: data.value,
				},
			});
		});
		bridge.on('reportError', (lastError) => {
			this.update(activityId, { lastError });
		});
		bridge.on('reportFinalized', () => {
			const current = this.getActivity(activityId) ?? createEmptyActivity(activityId);
			this.update(activityId, {
				moduleState: {
					...current.moduleState,
					isDirty: false,
					status: 'finalized',
				},
			});
		});

		const launchConfig: ReportLaunchConfig = {
			token,
			caseId: context.caseId,
			accession: context.accession,
			userId: context.userId,
			reportUrl: definition.path,
			reportOrigin: window.location.origin,
			role: context.role,
		};

		this.register(activityId, {
			activityId,
			status: 'idle',
			caseId: context.caseId,
			accession: context.accession,
			bridge,
			moduleState: {
				isDirty: false,
				status: 'draft',
			},
			lastError: null,
			visible: true,
			launchConfig,
		});
	}

	private async mintToken(): Promise<string | null> {
		try {
			const res = await fetch('/auth/token', {
				method: 'POST',
				credentials: 'include',
				headers: csrfHeaders(),
			});
			if (!res.ok) {
				console.error('[ActivityStore] Token mint failed:', res.status);
				return null;
			}
			const data: TokenResponse = await res.json();
			return data.accessToken;
		} catch (error) {
			console.error('[ActivityStore] Token mint error:', error);
			return null;
		}
	}
}

export const activityStore = new ActivityStore();
