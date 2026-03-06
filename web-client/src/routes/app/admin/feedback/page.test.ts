import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, fireEvent, waitFor } from '@testing-library/svelte';
import FeedbackPage from './+page.svelte';
import { adminStore } from '$lib/stores/admin.svelte';
import type { FeedbackSummary, FeedbackDetail, PageResponse } from '$lib/types/admin';

// ── Fixtures ────────────────────────────────────────────────────

function makeSummary(overrides: Partial<FeedbackSummary> = {}): FeedbackSummary {
	return {
		feedbackId: 'fb-001',
		submitterName: 'Dr. Smith',
		submitterEmail: 'smith@hospital.org',
		category: 'bug_report',
		bodyPreview: 'The annotation tool crashes when drawing polygons...',
		status: 'pending',
		createdAt: '2026-03-01T12:00:00Z',
		...overrides,
	};
}

function makeDetail(overrides: Partial<FeedbackDetail> = {}): FeedbackDetail {
	return {
		feedbackId: 'fb-001',
		identityId: 'id-001',
		submitterName: 'Dr. Smith',
		submitterEmail: 'smith@hospital.org',
		category: 'bug_report',
		body: 'The annotation tool crashes when I try to draw a polygon on a 40x zoom level.',
		context: { route: '/app/case/C-2026-001', viewerOpen: true, screenSize: '1920x1080' },
		status: 'pending',
		adminNotes: null,
		createdAt: '2026-03-01T12:00:00Z',
		acknowledgedAt: null,
		archivedAt: null,
		...overrides,
	};
}

function mockFetchPage(items: FeedbackSummary[], totalElements?: number) {
	const page: PageResponse<FeedbackSummary> = {
		content: items,
		page: 0,
		size: 20,
		totalElements: totalElements ?? items.length,
		totalPages: Math.ceil((totalElements ?? items.length) / 20),
	};
	return vi.fn().mockResolvedValue({
		ok: true,
		status: 200,
		json: () => Promise.resolve(page),
	});
}

function mockFetchDetail(detail: FeedbackDetail) {
	return vi.fn().mockResolvedValue({
		ok: true,
		status: 200,
		json: () => Promise.resolve(detail),
	});
}

function mockFetchNoContent() {
	return vi.fn().mockResolvedValue({
		ok: true,
		status: 204,
		json: () => Promise.resolve(undefined),
	});
}

// Chain multiple mock responses in order
function mockFetchSequence(...responses: Array<{ data: unknown; status?: number }>) {
	const fn = vi.fn();
	responses.forEach((r, i) => {
		fn.mockResolvedValueOnce({
			ok: (r.status ?? 200) >= 200 && (r.status ?? 200) < 300,
			status: r.status ?? 200,
			json: () => Promise.resolve(r.data),
		});
	});
	return fn;
}

// ── Helpers ─────────────────────────────────────────────────────

function resetStore() {
	adminStore.feedback = [];
	adminStore.feedbackPage = 0;
	adminStore.feedbackTotalPages = 0;
	adminStore.feedbackTotalElements = 0;
	adminStore.selectedFeedback = null;
	adminStore.isLoading = false;
	adminStore.error = null;
}

// ── Tests ───────────────────────────────────────────────────────

describe('Admin Feedback Page', () => {
	let originalFetch: typeof globalThis.fetch;

	beforeEach(() => {
		originalFetch = globalThis.fetch;
		resetStore();
		Object.defineProperty(document, 'cookie', {
			writable: true,
			value: 'XSRF-TOKEN=test-csrf-token',
		});
	});

	afterEach(() => {
		globalThis.fetch = originalFetch;
	});

	// ── Table rendering ─────────────────────────────────────────

	describe('table rendering', () => {
		it('shows empty state when no feedback', async () => {
			globalThis.fetch = mockFetchPage([]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('No feedback found');
			});
		});

		it('renders feedback items in the table', async () => {
			const items = [
				makeSummary({ feedbackId: 'fb-001', submitterName: 'Dr. Smith' }),
				makeSummary({ feedbackId: 'fb-002', submitterName: 'Dr. Jones', category: 'suggestion', status: 'acknowledged' }),
			];
			globalThis.fetch = mockFetchPage(items);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
				expect(container.textContent).toContain('Dr. Jones');
			});
		});

		it('shows submitter email under name', async () => {
			globalThis.fetch = mockFetchPage([makeSummary()]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('smith@hospital.org');
			});
		});

		it('shows body preview', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ bodyPreview: 'Zoom tool freezes at 60x' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Zoom tool freezes at 60x');
			});
		});

		it('shows total element count', async () => {
			globalThis.fetch = mockFetchPage([makeSummary()], 42);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('42 items');
			});
		});
	});

	// ── Category badges ─────────────────────────────────────────

	describe('category badges', () => {
		it('renders bug_report with red styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ category: 'bug_report' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badge = Array.from(container.querySelectorAll('span')).find(
					el => el.textContent?.trim() === 'bug report'
				);
				expect(badge).not.toBeUndefined();
				expect(badge!.className).toContain('text-red-500');
			});
		});

		it('renders suggestion with blue styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ category: 'suggestion' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badge = Array.from(container.querySelectorAll('span')).find(
					el => el.textContent?.trim() === 'suggestion'
				);
				expect(badge).not.toBeUndefined();
				expect(badge!.className).toContain('text-blue-500');
			});
		});

		it('renders question with amber styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ category: 'question' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badge = Array.from(container.querySelectorAll('span')).find(
					el => el.textContent?.trim() === 'question'
				);
				expect(badge).not.toBeUndefined();
				expect(badge!.className).toContain('text-amber-500');
			});
		});

		it('renders general with gray styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ category: 'general' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badge = Array.from(container.querySelectorAll('span')).find(
					el => el.textContent?.trim() === 'general'
				);
				expect(badge).not.toBeUndefined();
				expect(badge!.className).toContain('text-gray-400');
			});
		});
	});

	// ── Status badges ───────────────────────────────────────────

	describe('status badges', () => {
		it('renders pending with amber styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'pending' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badges = Array.from(container.querySelectorAll('span')).filter(
					el => el.textContent?.trim() === 'pending'
				);
				const statusBadge = badges.find(el => el.className.includes('font-semibold'));
				expect(statusBadge).not.toBeUndefined();
				expect(statusBadge!.className).toContain('text-amber-500');
			});
		});

		it('renders acknowledged with blue styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'acknowledged' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badges = Array.from(container.querySelectorAll('span')).filter(
					el => el.textContent?.trim() === 'acknowledged'
				);
				const statusBadge = badges.find(el => el.className.includes('font-semibold'));
				expect(statusBadge).not.toBeUndefined();
				expect(statusBadge!.className).toContain('text-blue-500');
			});
		});

		it('renders archived with gray styling', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'archived' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const badges = Array.from(container.querySelectorAll('span')).filter(
					el => el.textContent?.trim() === 'archived'
				);
				const statusBadge = badges.find(el => el.className.includes('font-semibold'));
				expect(statusBadge).not.toBeUndefined();
				expect(statusBadge!.className).toContain('text-gray-400');
			});
		});
	});

	// ── Status filter ───────────────────────────────────────────

	describe('status filter', () => {
		it('reloads with status param when filter changes', async () => {
			const fetchMock = mockFetchPage([]);
			globalThis.fetch = fetchMock;
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(fetchMock).toHaveBeenCalled();
			});

			// Change the select to 'pending'
			const select = container.querySelector('select')!;
			select.value = 'pending';
			await fireEvent.change(select);

			await waitFor(() => {
				// Second call should include status=pending
				const lastCall = fetchMock.mock.calls[fetchMock.mock.calls.length - 1];
				expect(lastCall[0]).toContain('status=pending');
			});
		});
	});

	// ── Expand / Detail ─────────────────────────────────────────

	describe('expand and detail', () => {
		it('shows full body and context when row is expanded', async () => {
			const items = [makeSummary()];
			const detail = makeDetail({ body: 'Full detailed description of the bug' });

			// First call: list, second call: detail
			globalThis.fetch = mockFetchSequence(
				{
					data: {
						content: items, page: 0, size: 20,
						totalElements: 1, totalPages: 1,
					},
				},
				{ data: detail },
			);

			const { container } = render(FeedbackPage);

			// Wait for table to render
			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			// Click the row to expand
			const row = container.querySelector('tbody tr')!;
			await fireEvent.click(row);

			await waitFor(() => {
				expect(container.textContent).toContain('Full detailed description of the bug');
			});
		});

		it('shows context JSON when expanded', async () => {
			const detail = makeDetail({
				context: { route: '/app/case/C-2026-001', viewerOpen: true },
			});

			globalThis.fetch = mockFetchSequence(
				{
					data: {
						content: [makeSummary()], page: 0, size: 20,
						totalElements: 1, totalPages: 1,
					},
				},
				{ data: detail },
			);

			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const row = container.querySelector('tbody tr')!;
			await fireEvent.click(row);

			await waitFor(() => {
				expect(container.textContent).toContain('/app/case/C-2026-001');
				expect(container.textContent).toContain('viewerOpen');
			});
		});

		it('shows admin notes textarea when expanded', async () => {
			globalThis.fetch = mockFetchSequence(
				{
					data: {
						content: [makeSummary()], page: 0, size: 20,
						totalElements: 1, totalPages: 1,
					},
				},
				{ data: makeDetail() },
			);

			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const row = container.querySelector('tbody tr')!;
			await fireEvent.click(row);

			await waitFor(() => {
				const textareas = container.querySelectorAll('textarea');
				expect(textareas.length).toBeGreaterThan(0);
				const notesArea = Array.from(textareas).find(
					ta => ta.getAttribute('placeholder')?.includes('notes')
				);
				expect(notesArea).not.toBeUndefined();
			});
		});
	});

	// ── Action buttons ──────────────────────────────────────────

	describe('action buttons', () => {
		it('shows Ack button for pending items', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'pending' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const ackBtn = Array.from(container.querySelectorAll('button')).find(
					b => b.textContent?.trim() === 'Ack'
				);
				expect(ackBtn).not.toBeUndefined();
			});
		});

		it('does not show Ack button for acknowledged items', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'acknowledged' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const ackBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Ack'
			);
			expect(ackBtn).toBeUndefined();
		});

		it('shows Archive button for non-archived items', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'pending' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const archiveBtn = Array.from(container.querySelectorAll('button')).find(
					b => b.textContent?.trim() === 'Archive'
				);
				expect(archiveBtn).not.toBeUndefined();
			});
		});

		it('does not show Archive button for already archived items', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'archived' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const archiveBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Archive'
			);
			expect(archiveBtn).toBeUndefined();
		});

		it('always shows Delete button regardless of status', async () => {
			globalThis.fetch = mockFetchPage([makeSummary({ status: 'archived' })]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				const deleteBtn = Array.from(container.querySelectorAll('button')).find(
					b => b.textContent?.trim() === 'Delete'
				);
				expect(deleteBtn).not.toBeUndefined();
			});
		});

		it('acknowledge button sends PATCH with acknowledged status', async () => {
			const items = [makeSummary({ status: 'pending' })];
			const fetchMock = mockFetchSequence(
				// Initial load
				{ data: { content: items, page: 0, size: 20, totalElements: 1, totalPages: 1 } },
				// PATCH response
				{ data: undefined, status: 204 },
				// Reload after update
				{ data: { content: [makeSummary({ status: 'acknowledged' })], page: 0, size: 20, totalElements: 1, totalPages: 1 } },
			);
			globalThis.fetch = fetchMock;
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const ackBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Ack'
			)!;
			await fireEvent.click(ackBtn);

			await waitFor(() => {
				// The PATCH call should have been made
				const patchCall = fetchMock.mock.calls.find(
					(c: any) => c[1]?.method === 'PATCH'
				);
				expect(patchCall).not.toBeUndefined();
				const body = JSON.parse(patchCall![1].body);
				expect(body.status).toBe('acknowledged');
			});
		});

		it('archive button sends PATCH with archived status', async () => {
			const items = [makeSummary({ status: 'pending' })];
			const fetchMock = mockFetchSequence(
				{ data: { content: items, page: 0, size: 20, totalElements: 1, totalPages: 1 } },
				{ data: undefined, status: 204 },
				{ data: { content: [makeSummary({ status: 'archived' })], page: 0, size: 20, totalElements: 1, totalPages: 1 } },
			);
			globalThis.fetch = fetchMock;
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const archiveBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Archive'
			)!;
			await fireEvent.click(archiveBtn);

			await waitFor(() => {
				const patchCall = fetchMock.mock.calls.find(
					(c: any) => c[1]?.method === 'PATCH'
				);
				expect(patchCall).not.toBeUndefined();
				const body = JSON.parse(patchCall![1].body);
				expect(body.status).toBe('archived');
			});
		});
	});

	// ── Delete with confirmation ────────────────────────────────

	describe('delete with confirmation', () => {
		it('shows confirmation dialog when Delete is clicked', async () => {
			globalThis.fetch = mockFetchPage([makeSummary()]);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			const deleteBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Delete'
			)!;
			await fireEvent.click(deleteBtn);

			await waitFor(() => {
				expect(container.textContent).toContain('Delete Feedback');
				expect(container.textContent).toContain('permanently delete');
			});
		});

		it('sends DELETE when confirmation is accepted', async () => {
			const fetchMock = mockFetchSequence(
				{ data: { content: [makeSummary()], page: 0, size: 20, totalElements: 1, totalPages: 1 } },
				{ data: undefined, status: 204 },
			);
			globalThis.fetch = fetchMock;
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			// Click delete to open dialog
			const deleteBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Delete'
			)!;
			await fireEvent.click(deleteBtn);

			// Find and click the confirm button in the dialog
			await waitFor(() => {
				expect(container.textContent).toContain('Delete Feedback');
			});

			// The confirm button in the dialog has the "Delete" text and red styling
			const confirmBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Delete' && b.className.includes('bg-red-600')
			)!;
			await fireEvent.click(confirmBtn);

			await waitFor(() => {
				const deleteCall = fetchMock.mock.calls.find(
					(c: any) => c[1]?.method === 'DELETE'
				);
				expect(deleteCall).not.toBeUndefined();
				expect(deleteCall![0]).toContain('/admin/feedback/fb-001');
			});
		});

		it('closes dialog on cancel without deleting', async () => {
			const fetchMock = mockFetchPage([makeSummary()]);
			globalThis.fetch = fetchMock;
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			// Click delete to open dialog
			const deleteBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Delete'
			)!;
			await fireEvent.click(deleteBtn);

			await waitFor(() => {
				expect(container.textContent).toContain('Delete Feedback');
			});

			// Click cancel
			const cancelBtn = Array.from(container.querySelectorAll('button')).find(
				b => b.textContent?.trim() === 'Cancel'
			)!;
			await fireEvent.click(cancelBtn);

			// Dialog should close — no extra fetch calls beyond initial load
			await waitFor(() => {
				expect(container.textContent).not.toContain('permanently delete');
			});

			// Only the initial load call should have been made
			expect(fetchMock.mock.calls.length).toBe(1);
		});
	});

	// ── Pagination ──────────────────────────────────────────────

	describe('pagination', () => {
		it('shows pagination when there are multiple pages', async () => {
			globalThis.fetch = mockFetchPage([makeSummary()], 42);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('42 total items');
				expect(container.textContent).toContain('1 / 3');
			});
		});

		it('hides pagination controls when only one page', async () => {
			globalThis.fetch = mockFetchPage([makeSummary()], 1);
			const { container } = render(FeedbackPage);

			await waitFor(() => {
				expect(container.textContent).toContain('Dr. Smith');
			});

			// Pagination buttons should not be rendered
			const paginationBtns = Array.from(container.querySelectorAll('button')).filter(
				b => b.textContent?.trim() === 'Prev' || b.textContent?.trim() === 'Next'
			);
			expect(paginationBtns).toHaveLength(0);
			expect(container.textContent).not.toContain('total items');
		});
	});
});
