/**
 * ViewerBridge Tests
 *
 * Models the exact user scenarios:
 * 1. Launch viewer → navigate to worklist → click same case → viewer should unpause
 * 2. Launch viewer → navigate to worklist → click different case → viewer should stay paused
 * 3. Launch viewer → navigate to admin → viewer should close
 * 4. Back button from worklist to case → viewer should unpause
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ViewerBridge, type ViewerBridgeState } from './viewer-bridge';

// ─── Helpers ───────────────────────────────────────────────

/** Create a mock popup window */
function createMockWindow(): Window {
	const win = {
		closed: false,
		close: vi.fn(() => {
			win.closed = true;
		}),
		postMessage: vi.fn(),
	};
	return win as unknown as Window;
}

/** Standard launch config */
function launchConfig(accession = 'S260005') {
	return {
		token: 'test-jwt-token',
		caseId: accession,
		accession,
		viewerUrl: '/viewer/orchestrated.html',
		viewerOrigin: 'http://localhost:5174',
		tileServerUrl: '/tiles',
		sessionServiceUrl: '/ws',
		userId: 'user-1',
	};
}

/** Simulate the viewer sending viewer:ready back to the orchestrator */
function simulateViewerReady(bridge: ViewerBridge, mockWindow: Window) {
	// Find the message handler registered on window
	const addEventListenerCalls = vi.mocked(window.addEventListener).mock.calls;
	const messageCall = addEventListenerCalls.find(([event]) => event === 'message');
	if (!messageCall) throw new Error('No message handler registered');

	const handler = messageCall[1] as (event: MessageEvent) => void;

	// Simulate viewer:ready message from the viewer window
	handler({
		data: { type: 'viewer:ready', payload: {} },
		origin: 'http://localhost:5174',
		source: mockWindow,
	} as unknown as MessageEvent);
}

// ─── Test Suite ────────────────────────────────────────────

describe('ViewerBridge', () => {
	let bridge: ViewerBridge;
	let mockWindow: Window;
	let stateChanges: ViewerBridgeState[];

	beforeEach(() => {
		vi.useFakeTimers();

		mockWindow = createMockWindow();
		vi.spyOn(window, 'open').mockReturnValue(mockWindow);
		vi.spyOn(window, 'addEventListener');
		vi.spyOn(window, 'removeEventListener');

		stateChanges = [];
		bridge = new ViewerBridge();
		bridge.on('stateChange', (s) => stateChanges.push(s));
	});

	afterEach(() => {
		bridge.destroy();
		vi.useRealTimers();
		vi.restoreAllMocks();
	});

	describe('launch lifecycle', () => {
		it('should open a popup window on launch', () => {
			bridge.launch(launchConfig());

			expect(window.open).toHaveBeenCalledWith(
				'/viewer/orchestrated.html',
				'starling-viewer',
				expect.any(String),
			);
			expect(stateChanges).toContain('launching');
		});

		it('should transition to connected when viewer sends ready', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);

			expect(bridge.getState()).toBe('connected');
			expect(stateChanges).toEqual(['launching', 'connected']);
		});

		it('should send orchestrator:init after viewer ready', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);

			const postMessageCalls = vi.mocked(mockWindow.postMessage).mock.calls;
			const initCall = postMessageCalls.find(
				([msg]) => (msg as { type: string }).type === 'orchestrator:init',
			);
			expect(initCall).toBeDefined();
			expect((initCall![0] as { payload: { caseId: string } }).payload.caseId).toBe('S260005');
		});
	});

	describe('focus state (blur/unpause)', () => {
		it('should send blurred state when focus set to false', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			vi.mocked(mockWindow.postMessage).mockClear();

			bridge.sendFocusState('blurred');

			const msg = vi.mocked(mockWindow.postMessage).mock.calls[0][0] as {
				type: string;
				payload: { state: string };
			};
			expect(msg.type).toBe('orchestrator:focus');
			expect(msg.payload.state).toBe('blurred');
		});

		it('should send active state when focus set to true', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			vi.mocked(mockWindow.postMessage).mockClear();

			bridge.sendFocusState('active');

			const msg = vi.mocked(mockWindow.postMessage).mock.calls[0][0] as {
				type: string;
				payload: { state: string };
			};
			expect(msg.type).toBe('orchestrator:focus');
			expect(msg.payload.state).toBe('active');
		});

		it('should NOT send focus state if not connected', () => {
			bridge.launch(launchConfig());
			// Still in 'launching' state, not 'connected'

			bridge.sendFocusState('blurred');

			// Only the messages from launch, nothing about focus
			const focusCalls = vi.mocked(mockWindow.postMessage).mock.calls.filter(
				([msg]) => (msg as { type: string }).type === 'orchestrator:focus',
			);
			expect(focusCalls).toHaveLength(0);
		});

		it('blur then active should work (worklist → same case scenario)', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			vi.mocked(mockWindow.postMessage).mockClear();

			// Navigate to worklist → blur
			bridge.sendFocusState('blurred');
			// Navigate back to same case → active
			bridge.sendFocusState('active');

			const focusCalls = vi.mocked(mockWindow.postMessage).mock.calls.filter(
				([msg]) => (msg as { type: string }).type === 'orchestrator:focus',
			);
			expect(focusCalls).toHaveLength(2);
			expect((focusCalls[0][0] as { payload: { state: string } }).payload.state).toBe('blurred');
			expect((focusCalls[1][0] as { payload: { state: string } }).payload.state).toBe('active');
		});
	});

	describe('close behavior', () => {
		it('close() should send logout and close the window', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			vi.mocked(mockWindow.postMessage).mockClear();

			bridge.close();

			// Should have sent orchestrator:logout
			const logoutCalls = vi.mocked(mockWindow.postMessage).mock.calls.filter(
				([msg]) => (msg as { type: string }).type === 'orchestrator:logout',
			);
			expect(logoutCalls).toHaveLength(1);

			// Window should be closed
			expect(mockWindow.close).toHaveBeenCalled();

			// State should be closed
			expect(bridge.getState()).toBe('closed');
		});

		it('after close, sendFocusState should be no-op', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			bridge.close();
			vi.mocked(mockWindow.postMessage).mockClear();

			bridge.sendFocusState('active');

			expect(vi.mocked(mockWindow.postMessage).mock.calls).toHaveLength(0);
		});

		it('after close, isAlive should return false', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);

			expect(bridge.isAlive()).toBe(true);

			bridge.close();

			expect(bridge.isAlive()).toBe(false);
		});
	});

	describe('window closed detection', () => {
		it('should detect when viewer window is closed by user', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);

			// Simulate user closing the popup
			(mockWindow as { closed: boolean }).closed = true;

			// The windowCheckInterval fires every 1 second
			vi.advanceTimersByTime(1500);

			expect(bridge.getState()).toBe('closed');
			expect(stateChanges).toContain('closed');
		});
	});

	describe('case change', () => {
		it('should send case-change message when connected', () => {
			bridge.launch(launchConfig());
			simulateViewerReady(bridge, mockWindow);
			vi.mocked(mockWindow.postMessage).mockClear();

			bridge.sendCaseChange('B260001', 'B260001');

			const msg = vi.mocked(mockWindow.postMessage).mock.calls[0][0] as {
				type: string;
				payload: { caseId: string; accession: string };
			};
			expect(msg.type).toBe('orchestrator:case-change');
			expect(msg.payload.caseId).toBe('B260001');
		});

		it('should NOT send case-change when not connected', () => {
			bridge.launch(launchConfig());
			// Still launching, not connected

			bridge.sendCaseChange('B260001', 'B260001');

			const changeCalls = vi.mocked(mockWindow.postMessage).mock.calls.filter(
				([msg]) => (msg as { type: string }).type === 'orchestrator:case-change',
			);
			expect(changeCalls).toHaveLength(0);
		});
	});

	describe('re-launch behavior', () => {
		it('launch() while connected should close old window and open new', () => {
			bridge.launch(launchConfig('S260005'));
			simulateViewerReady(bridge, mockWindow);

			const oldWindow = mockWindow;
			const newWindow = createMockWindow();
			vi.mocked(window.open).mockReturnValue(newWindow);

			// Re-launch (this is what happens when launchViewer falls through guards)
			bridge.launch(launchConfig('B260001'));

			expect(oldWindow.close).toHaveBeenCalled();
			expect(window.open).toHaveBeenCalledTimes(2);
		});
	});
});

// ─── Navigation Zone Logic (pure function test) ────────────

describe('Navigation zone logic', () => {
	/** Replicate the safe-zone check from +layout.svelte */
	function isSafeZone(pathname: string): boolean {
		return pathname.startsWith('/app/case/') || pathname.startsWith('/app/worklist');
	}

	function isOnActiveCase(pathname: string, currentCase: string): boolean {
		return pathname === `/app/case/${currentCase}`;
	}

	describe('safe zone detection', () => {
		it('/app/case/S260005 is a safe zone', () => {
			expect(isSafeZone('/app/case/S260005')).toBe(true);
		});

		it('/app/case/B260001 is a safe zone', () => {
			expect(isSafeZone('/app/case/B260001')).toBe(true);
		});

		it('/app/worklist is a safe zone', () => {
			expect(isSafeZone('/app/worklist')).toBe(true);
		});

		it('/app/worklist/ (trailing slash) is a safe zone', () => {
			expect(isSafeZone('/app/worklist/')).toBe(true);
		});

		it('/app/admin is NOT a safe zone', () => {
			expect(isSafeZone('/app/admin')).toBe(false);
		});

		it('/app/hat is NOT a safe zone', () => {
			expect(isSafeZone('/app/hat')).toBe(false);
		});

		it('/app/settings is NOT a safe zone', () => {
			expect(isSafeZone('/app/settings')).toBe(false);
		});

		it('/app is NOT a safe zone (dashboard)', () => {
			expect(isSafeZone('/app')).toBe(false);
		});

		it('/app/ai is NOT a safe zone', () => {
			expect(isSafeZone('/app/ai')).toBe(false);
		});

		it('/app/case/ (no accession) is still a safe zone', () => {
			expect(isSafeZone('/app/case/')).toBe(true);
		});
	});

	describe('active case detection', () => {
		it('matches when pathname equals /app/case/{currentCase}', () => {
			expect(isOnActiveCase('/app/case/S260005', 'S260005')).toBe(true);
		});

		it('does not match different case', () => {
			expect(isOnActiveCase('/app/case/B260001', 'S260005')).toBe(false);
		});

		it('does not match worklist', () => {
			expect(isOnActiveCase('/app/worklist', 'S260005')).toBe(false);
		});

		it('does not match admin', () => {
			expect(isOnActiveCase('/app/admin', 'S260005')).toBe(false);
		});
	});

	describe('user scenario: focus decisions (viewer never closed by navigation)', () => {
		/**
		 * Replicate the $effect logic from +layout.svelte.
		 * The viewer is NEVER closed by navigation — only active/blurred.
		 * Returns the focus action taken, or null if guard fails.
		 */
		function focusDecision(
			viewerIsOpen: boolean,
			currentCase: string | null,
			pathname: string,
		): 'active' | 'blurred' | null {
			if (!viewerIsOpen || !currentCase) return null;
			return isOnActiveCase(pathname, currentCase) ? 'active' : 'blurred';
		}

		it('Scenario 1: Case A → worklist → same case A (click)', () => {
			const currentCase = 'S260005';
			// Step 1: On case A — viewer active
			expect(focusDecision(true, currentCase, '/app/case/S260005')).toBe('active');

			// Step 2: Navigate to worklist — viewer blurred
			expect(focusDecision(true, currentCase, '/app/worklist')).toBe('blurred');

			// Step 3: Click same case A — viewer active again!
			expect(focusDecision(true, currentCase, '/app/case/S260005')).toBe('active');
		});

		it('Scenario 2: Case A → worklist → different case B', () => {
			const currentCase = 'S260005';

			expect(focusDecision(true, currentCase, '/app/worklist')).toBe('blurred');

			// Click case B — viewer still blurred (not active case)
			expect(focusDecision(true, currentCase, '/app/case/B260001')).toBe('blurred');
		});

		it('Scenario 3: Case A → admin (viewer stays open, blurred)', () => {
			const currentCase = 'S260005';
			expect(focusDecision(true, currentCase, '/app/admin')).toBe('blurred');
		});

		it('Scenario 4: Case A → HAT (viewer stays open, blurred)', () => {
			const currentCase = 'S260005';
			expect(focusDecision(true, currentCase, '/app/hat')).toBe('blurred');
		});

		it('Scenario 5: Case A → settings (viewer stays open, blurred)', () => {
			const currentCase = 'S260005';
			expect(focusDecision(true, currentCase, '/app/settings')).toBe('blurred');
		});

		it('Scenario 6: Case A → dashboard /app (viewer stays open, blurred)', () => {
			const currentCase = 'S260005';
			expect(focusDecision(true, currentCase, '/app')).toBe('blurred');
		});

		it('Scenario 7: Viewer not open — focus decisions are null', () => {
			expect(focusDecision(false, 'S260005', '/app/admin')).toBeNull();
			expect(focusDecision(false, 'S260005', '/app/worklist')).toBeNull();
		});

		it('Scenario 8: Case A → worklist → back button → case A (same as click)', () => {
			const currentCase = 'S260005';

			// Go to worklist — blurred
			expect(focusDecision(true, currentCase, '/app/worklist')).toBe('blurred');

			// Back button to case A — active
			expect(focusDecision(true, currentCase, '/app/case/S260005')).toBe('active');
		});
	});
});
