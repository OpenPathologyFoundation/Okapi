/**
 * Viewer Store — Tracks the viewer bridge state across the orchestrator app.
 *
 * Owns the singleton ViewerBridge instance. Provides reactive state
 * so that the case page, header, and worklist can all show viewer status.
 */

import { browser } from '$app/environment';
import { csrfHeaders } from '$lib/csrf';
import { ViewerBridge, type ViewerBridgeState } from '$lib/viewer-bridge';
import type { ViewerLaunchConfig } from '$lib/types/viewer-bridge';

interface TokenResponse {
	accessToken: string;
	tokenType: string;
	expiresInSeconds: number;
	okapiAuthzVersion: string;
}

class ViewerStore {
	/** Bridge instance (null until first launch) */
	bridge: ViewerBridge | null = $state(null);

	/** Current bridge state */
	state: ViewerBridgeState = $state('idle');

	/** Accession of the case currently open in the viewer */
	currentCase: string | null = $state(null);

	/** Number of slides in the current viewer case */
	slideCount: number | null = $state(null);

	/** Last error from the viewer */
	lastError: { code: string; message: string } | null = $state(null);

	/** Whether the viewer window is alive */
	isOpen = $derived(this.state === 'connected' || this.state === 'launching');

	/**
	 * Launch the viewer for a given case.
	 *
	 * If the viewer is already connected, sends a case-change message instead.
	 * If the viewer is launching, does nothing (prevents double-open).
	 */
	async launchViewer(config: Omit<ViewerLaunchConfig, 'token'>): Promise<void> {
		console.log('[ViewerStore] launchViewer — state=' + this.state + ', bridge=' + !!this.bridge + ', accession=' + config.accession);
		if (!browser) return;

		// If already connected, just switch case
		if (this.bridge && this.state === 'connected') {
			this.bridge.sendCaseChange(config.caseId, config.accession);
			this.currentCase = config.accession;
			this.slideCount = null;
			this.lastError = null;
			return;
		}

		// Prevent double-open while launching
		if (this.state === 'launching') return;

		// Mint a fresh JWT
		const token = await this.mintToken();
		if (!token) {
			this.lastError = { code: 'TOKEN_MINT_FAILED', message: 'Failed to obtain viewer token' };
			return;
		}

		// Create or reuse bridge
		if (!this.bridge) {
			this.bridge = new ViewerBridge({
				mintToken: () => this.mintToken(),
			});

			// Subscribe to bridge events
			this.bridge.on('stateChange', (s) => {
				this.state = s;
			});
			this.bridge.on('caseLoaded', (data) => {
				this.slideCount = data.slideCount;
			});
			this.bridge.on('viewerError', (data) => {
				this.lastError = data;
			});
		}

		this.currentCase = config.accession;
		this.slideCount = null;
		this.lastError = null;

		this.bridge.launch({
			...config,
			token,
		});
	}

	/** Send focus/blur state to the viewer */
	setViewerFocus(focused: boolean): void {
		if (this.bridge && this.state === 'connected') {
			this.bridge.sendFocusState(focused ? 'active' : 'blurred');
		}
	}

	/** Send a case-change to the already-open viewer */
	sendCaseChange(caseId: string, accession: string): void {
		if (this.bridge && this.state === 'connected') {
			this.bridge.sendCaseChange(caseId, accession);
			this.currentCase = accession;
			this.slideCount = null;
			this.lastError = null;
		}
	}

	/** Close the viewer window */
	closeViewer(): void {
		console.warn('[ViewerStore] closeViewer() called — state=' + this.state + ', currentCase=' + this.currentCase, new Error().stack);
		if (this.bridge) {
			this.bridge.close();
		}
		this.currentCase = null;
		this.slideCount = null;
		this.lastError = null;
	}

	/** Destroy bridge and all resources */
	destroy(): void {
		if (this.bridge) {
			this.bridge.destroy();
			this.bridge = null;
		}
		this.state = 'idle';
		this.currentCase = null;
		this.slideCount = null;
		this.lastError = null;
	}

	/** Mint a JWT via the auth backend */
	private async mintToken(): Promise<string | null> {
		try {
			const res = await fetch('/auth/token', {
				method: 'POST',
				credentials: 'include',
				headers: csrfHeaders(),
			});
			if (!res.ok) {
				console.error('[ViewerStore] Token mint failed:', res.status);
				return null;
			}
			const data: TokenResponse = await res.json();
			return data.accessToken;
		} catch (error) {
			console.error('[ViewerStore] Token mint error:', error);
			return null;
		}
	}
}

export const viewerStore = new ViewerStore();
