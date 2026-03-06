import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { adminStore } from './admin.svelte';
import type { FeedbackSummary, FeedbackDetail, PageResponse, DashboardSummary } from '$lib/types/admin';

// ── Test fixtures ───────────────────────────────────────────────

function makeFeedbackSummary(overrides: Partial<FeedbackSummary> = {}): FeedbackSummary {
	return {
		feedbackId: 'fb-001',
		submitterName: 'Dr. Smith',
		submitterEmail: 'smith@hospital.org',
		category: 'bug_report',
		bodyPreview: 'The annotation tool crashes when...',
		status: 'pending',
		createdAt: '2026-03-01T12:00:00Z',
		...overrides,
	};
}

function makeFeedbackDetail(overrides: Partial<FeedbackDetail> = {}): FeedbackDetail {
	return {
		feedbackId: 'fb-001',
		identityId: 'id-001',
		submitterName: 'Dr. Smith',
		submitterEmail: 'smith@hospital.org',
		category: 'bug_report',
		body: 'The annotation tool crashes when I try to draw a polygon on a 40x zoom level. It happens consistently.',
		context: { route: '/app/case/C-2026-001', viewerOpen: true, screenSize: '1920x1080' },
		status: 'pending',
		adminNotes: null,
		createdAt: '2026-03-01T12:00:00Z',
		acknowledgedAt: null,
		archivedAt: null,
		...overrides,
	};
}

function makeDashboard(overrides: Partial<DashboardSummary> = {}): DashboardSummary {
	return {
		totalIdentities: 25,
		activeIdentities: 20,
		totalRoles: 5,
		activeDevices: 12,
		activeBreakGlassGrants: 1,
		activeResearchGrants: 3,
		pendingFeedback: 7,
		...overrides,
	};
}

function makePage<T>(content: T[], totalElements = content.length): PageResponse<T> {
	return {
		content,
		page: 0,
		size: 20,
		totalElements,
		totalPages: Math.ceil(totalElements / 20),
	};
}

// ── Helpers ─────────────────────────────────────────────────────

function mockFetch(response: unknown, status = 200) {
	return vi.fn().mockResolvedValue({
		ok: status >= 200 && status < 300,
		status,
		json: () => Promise.resolve(response),
	});
}

function resetStore() {
	adminStore.feedback = [];
	adminStore.feedbackPage = 0;
	adminStore.feedbackTotalPages = 0;
	adminStore.feedbackTotalElements = 0;
	adminStore.selectedFeedback = null;
	adminStore.dashboard = null;
	adminStore.isLoading = false;
	adminStore.error = null;
}

// ── Tests ───────────────────────────────────────────────────────

describe('AdminStore — Feedback', () => {
	let originalFetch: typeof globalThis.fetch;

	beforeEach(() => {
		originalFetch = globalThis.fetch;
		resetStore();
	});

	afterEach(() => {
		globalThis.fetch = originalFetch;
	});

	// ── loadFeedback ────────────────────────────────────────────

	describe('loadFeedback', () => {
		it('fetches all feedback and updates state', async () => {
			const items = [makeFeedbackSummary(), makeFeedbackSummary({ feedbackId: 'fb-002', status: 'acknowledged' })];
			globalThis.fetch = mockFetch(makePage(items, 42));

			await adminStore.loadFeedback();

			expect(adminStore.feedback).toHaveLength(2);
			expect(adminStore.feedbackTotalElements).toBe(42);
			expect(adminStore.feedbackTotalPages).toBe(3);
			expect(adminStore.error).toBeNull();
		});

		it('passes status filter as query param', async () => {
			const fetchMock = mockFetch(makePage([]));
			globalThis.fetch = fetchMock;

			await adminStore.loadFeedback('pending', 0, 20);

			const url = fetchMock.mock.calls[0][0] as string;
			expect(url).toContain('status=pending');
		});

		it('passes pagination params', async () => {
			const fetchMock = mockFetch(makePage([]));
			globalThis.fetch = fetchMock;

			await adminStore.loadFeedback(undefined, 2, 10);

			const url = fetchMock.mock.calls[0][0] as string;
			expect(url).toContain('page=2');
			expect(url).toContain('size=10');
		});

		it('does not include status param when not provided', async () => {
			const fetchMock = mockFetch(makePage([]));
			globalThis.fetch = fetchMock;

			await adminStore.loadFeedback();

			const url = fetchMock.mock.calls[0][0] as string;
			expect(url).not.toContain('status=');
		});

		it('sets error on failed fetch', async () => {
			globalThis.fetch = mockFetch({ message: 'Forbidden' }, 403);

			await adminStore.loadFeedback();

			expect(adminStore.error).toBe('Forbidden');
			expect(adminStore.feedback).toHaveLength(0);
		});

		it('handles network error gracefully', async () => {
			globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network error'));

			await adminStore.loadFeedback();

			expect(adminStore.error).toBe('Network error');
		});
	});

	// ── loadFeedbackDetail ──────────────────────────────────────

	describe('loadFeedbackDetail', () => {
		it('fetches detail and sets selectedFeedback', async () => {
			const detail = makeFeedbackDetail();
			globalThis.fetch = mockFetch(detail);

			await adminStore.loadFeedbackDetail('fb-001');

			expect(adminStore.selectedFeedback).not.toBeNull();
			expect(adminStore.selectedFeedback!.feedbackId).toBe('fb-001');
			expect(adminStore.selectedFeedback!.body).toContain('annotation tool crashes');
			expect(adminStore.selectedFeedback!.context).toHaveProperty('route');
		});

		it('calls correct URL with feedback ID', async () => {
			const fetchMock = mockFetch(makeFeedbackDetail());
			globalThis.fetch = fetchMock;

			await adminStore.loadFeedbackDetail('fb-999');

			const url = fetchMock.mock.calls[0][0] as string;
			expect(url).toBe('/admin/feedback/fb-999');
		});

		it('sets error when detail fetch fails', async () => {
			globalThis.fetch = mockFetch({ message: 'Not found' }, 404);

			await adminStore.loadFeedbackDetail('fb-missing');

			expect(adminStore.selectedFeedback).toBeNull();
			expect(adminStore.error).toBe('Not found');
		});
	});

	// ── updateFeedback ──────────────────────────────────────────

	describe('updateFeedback', () => {
		it('sends PATCH with status', async () => {
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.updateFeedback('fb-001', 'acknowledged');

			const [url, init] = fetchMock.mock.calls[0];
			expect(url).toBe('/admin/feedback/fb-001');
			expect(init.method).toBe('PATCH');
			const body = JSON.parse(init.body as string);
			expect(body.status).toBe('acknowledged');
		});

		it('sends PATCH with adminNotes', async () => {
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.updateFeedback('fb-001', undefined, 'Investigating this issue');

			const body = JSON.parse(fetchMock.mock.calls[0][1].body as string);
			expect(body.adminNotes).toBe('Investigating this issue');
			expect(body.status).toBeUndefined();
		});

		it('sends PATCH with both status and notes', async () => {
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.updateFeedback('fb-001', 'archived', 'Resolved in v2.1');

			const body = JSON.parse(fetchMock.mock.calls[0][1].body as string);
			expect(body.status).toBe('archived');
			expect(body.adminNotes).toBe('Resolved in v2.1');
		});

		it('sets error on failed update', async () => {
			globalThis.fetch = mockFetch({ message: 'Server error' }, 500);

			await adminStore.updateFeedback('fb-001', 'acknowledged');

			expect(adminStore.error).toBe('Server error');
		});
	});

	// ── deleteFeedback ──────────────────────────────────────────

	describe('deleteFeedback', () => {
		it('sends DELETE and removes item from local state', async () => {
			adminStore.feedback = [
				makeFeedbackSummary({ feedbackId: 'fb-001' }),
				makeFeedbackSummary({ feedbackId: 'fb-002' }),
				makeFeedbackSummary({ feedbackId: 'fb-003' }),
			];
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.deleteFeedback('fb-002');

			expect(fetchMock.mock.calls[0][0]).toBe('/admin/feedback/fb-002');
			expect(fetchMock.mock.calls[0][1].method).toBe('DELETE');
			expect(adminStore.feedback).toHaveLength(2);
			expect(adminStore.feedback.find(f => f.feedbackId === 'fb-002')).toBeUndefined();
		});

		it('preserves other items when deleting', async () => {
			adminStore.feedback = [
				makeFeedbackSummary({ feedbackId: 'fb-001' }),
				makeFeedbackSummary({ feedbackId: 'fb-002' }),
			];
			globalThis.fetch = mockFetch(undefined, 204);

			await adminStore.deleteFeedback('fb-001');

			expect(adminStore.feedback).toHaveLength(1);
			expect(adminStore.feedback[0].feedbackId).toBe('fb-002');
		});

		it('sets error on failed delete', async () => {
			adminStore.feedback = [makeFeedbackSummary()];
			globalThis.fetch = mockFetch({ message: 'Forbidden' }, 403);

			await adminStore.deleteFeedback('fb-001');

			expect(adminStore.error).toBe('Forbidden');
			// Should NOT remove on failure
			expect(adminStore.feedback).toHaveLength(1);
		});
	});

	// ── Dashboard with pendingFeedback ──────────────────────────

	describe('loadDashboard (pendingFeedback field)', () => {
		it('includes pendingFeedback count', async () => {
			globalThis.fetch = mockFetch(makeDashboard({ pendingFeedback: 42 }));

			await adminStore.loadDashboard();

			expect(adminStore.dashboard).not.toBeNull();
			expect(adminStore.dashboard!.pendingFeedback).toBe(42);
		});

		it('pendingFeedback is 0 when no feedback exists', async () => {
			globalThis.fetch = mockFetch(makeDashboard({ pendingFeedback: 0 }));

			await adminStore.loadDashboard();

			expect(adminStore.dashboard!.pendingFeedback).toBe(0);
		});
	});

	// ── CSRF headers on mutating requests ───────────────────────

	describe('CSRF headers', () => {
		it('includes CSRF headers on PATCH requests', async () => {
			// Set up the XSRF-TOKEN cookie so csrfHeaders() returns something
			Object.defineProperty(document, 'cookie', {
				writable: true,
				value: 'XSRF-TOKEN=test-csrf-token',
			});
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.updateFeedback('fb-001', 'acknowledged');

			const headers = fetchMock.mock.calls[0][1].headers as Record<string, string>;
			expect(headers['X-XSRF-TOKEN']).toBe('test-csrf-token');
		});

		it('includes CSRF headers on DELETE requests', async () => {
			Object.defineProperty(document, 'cookie', {
				writable: true,
				value: 'XSRF-TOKEN=test-csrf-token',
			});
			adminStore.feedback = [makeFeedbackSummary()];
			const fetchMock = mockFetch(undefined, 204);
			globalThis.fetch = fetchMock;

			await adminStore.deleteFeedback('fb-001');

			const headers = fetchMock.mock.calls[0][1].headers as Record<string, string>;
			expect(headers['X-XSRF-TOKEN']).toBe('test-csrf-token');
		});

		it('does NOT include CSRF headers on GET requests', async () => {
			Object.defineProperty(document, 'cookie', {
				writable: true,
				value: 'XSRF-TOKEN=test-csrf-token',
			});
			const fetchMock = mockFetch(makePage([]));
			globalThis.fetch = fetchMock;

			await adminStore.loadFeedback();

			const headers = fetchMock.mock.calls[0][1].headers as Record<string, string>;
			expect(headers['X-XSRF-TOKEN']).toBeUndefined();
		});
	});

	// ── Loading state management ────────────────────────────────

	describe('loading state', () => {
		it('sets isLoading to true during request', async () => {
			let resolvePromise: (value: Response) => void;
			globalThis.fetch = vi.fn().mockReturnValue(new Promise(r => { resolvePromise = r; }));

			const promise = adminStore.loadFeedback();

			expect(adminStore.isLoading).toBe(true);

			resolvePromise!({
				ok: true,
				status: 200,
				json: () => Promise.resolve(makePage([])),
			} as Response);

			await promise;
			expect(adminStore.isLoading).toBe(false);
		});

		it('clears error before new request', async () => {
			adminStore.error = 'previous error';
			globalThis.fetch = mockFetch(makePage([]));

			await adminStore.loadFeedback();

			expect(adminStore.error).toBeNull();
		});
	});
});
