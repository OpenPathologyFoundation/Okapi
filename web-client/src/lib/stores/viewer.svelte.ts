/**
 * Viewer Store compatibility facade.
 *
 * Preserves the existing viewerStore API while the underlying lifecycle and
 * tracking move into the generic activityStore.
 */

import { activityStore } from '$lib/stores/activity.svelte';
import type { ViewerBridgeState } from '$lib/viewer-bridge';
import type { ViewerLaunchConfig, ViewerMode } from '$lib/types/viewer-bridge';

const VIEWER_ACTIVITY_ID = 'pelican-viewer';

class ViewerStore {
	private get activity() {
		return activityStore.getActivity(VIEWER_ACTIVITY_ID);
	}

	get bridge() {
		return this.activity?.bridge ?? null;
	}

	get state(): ViewerBridgeState {
		return (this.activity?.status ?? 'idle') as ViewerBridgeState;
	}

	get currentCase(): string | null {
		return this.activity?.accession ?? null;
	}

	get slideCount(): number | null {
		return (this.activity?.moduleState.slideCount as number | undefined) ?? null;
	}

	get currentMode(): ViewerMode | null {
		return (this.activity?.moduleState.mode as ViewerMode | undefined) ?? null;
	}

	get lastError(): { code: string; message: string } | null {
		return this.activity?.lastError ?? null;
	}

	get isOpen(): boolean {
		return this.state === 'connected' || this.state === 'launching';
	}

	async launchViewer(config: Omit<ViewerLaunchConfig, 'token'>): Promise<void> {
		await activityStore.launchActivity(VIEWER_ACTIVITY_ID, config);
	}

	setViewerFocus(focused: boolean): void {
		activityStore.focusActivity(VIEWER_ACTIVITY_ID, focused);
	}

	sendCaseChange(caseId: string, accession: string): void {
		activityStore.sendCaseChange(VIEWER_ACTIVITY_ID, caseId, accession);
	}

	closeViewer(): void {
		activityStore.closeActivity(VIEWER_ACTIVITY_ID);
	}

	destroy(): void {
		activityStore.closeActivity(VIEWER_ACTIVITY_ID);
	}
}

export const viewerStore = new ViewerStore();
