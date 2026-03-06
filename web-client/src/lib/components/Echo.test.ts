import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, fireEvent, waitFor } from '@testing-library/svelte';
import Echo from './Echo.svelte';
import { page } from '$app/state';

// ── Helpers ─────────────────────────────────────────────────────

function mockFetchOk(response: unknown = {}, status = 200) {
	return vi.fn().mockResolvedValue({
		ok: true,
		status,
		json: () => Promise.resolve(response),
	});
}

function mockFetchFail(status = 500) {
	return vi.fn().mockResolvedValue({
		ok: false,
		status,
		json: () => Promise.resolve({}),
	});
}

async function openEcho(container: HTMLElement) {
	const anchor = container.querySelector('button')!;
	await fireEvent.click(anchor);
	// Wait for the modal to appear (transition)
	await waitFor(() => {
		expect(container.querySelector('textarea') ?? container.querySelector('.text-amber-500'))
			.not.toBeNull();
	});
}

async function typeAndSubmit(container: HTMLElement, text: string) {
	const textarea = container.querySelector('textarea')!;
	textarea.value = text;
	await fireEvent.input(textarea);
	await fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });
}

// ── Tests ───────────────────────────────────────────────────────

describe('Echo component', () => {
	let originalFetch: typeof globalThis.fetch;

	beforeEach(() => {
		originalFetch = globalThis.fetch;
		Object.defineProperty(document, 'cookie', {
			writable: true,
			value: 'XSRF-TOKEN=test-csrf-token',
		});
		page.url = new URL('http://localhost/app/case/C-2026-001') as any;
		page.params = { accession: 'C-2026-001' };
	});

	afterEach(() => {
		globalThis.fetch = originalFetch;
	});

	// ── Rendering ───────────────────────────────────────────────

	describe('rendering', () => {
		it('renders the anchor button with mic icon', () => {
			const { container } = render(Echo);
			const button = container.querySelector('button');
			expect(button).not.toBeNull();
			expect(button!.getAttribute('title')).toContain('feedback');
		});

		it('does not show modal by default', () => {
			const { container } = render(Echo);
			expect(container.querySelector('textarea')).toBeNull();
		});

		it('opens modal when anchor button is clicked', async () => {
			const { container } = render(Echo);
			await openEcho(container);
			expect(container.querySelector('textarea')).not.toBeNull();
		});

		it('shows current route in the header context', async () => {
			page.url = new URL('http://localhost/app/case/C-2026-042') as any;
			const { container } = render(Echo);
			await openEcho(container);
			const header = container.querySelector('.font-mono');
			expect(header?.textContent).toContain('/app/case/C-2026-042');
		});

		it('opens modal on tilde key press', async () => {
			const { container } = render(Echo);
			await fireEvent.keyDown(window, { key: '~' });
			await waitFor(() => {
				expect(container.querySelector('textarea')).not.toBeNull();
			});
		});
	});

	// ── Submit flow ─────────────────────────────────────────────

	describe('submit flow', () => {
		it('sends POST to /api/feedback with correct body', async () => {
			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'The zoom tool is laggy');

			await waitFor(() => {
				expect(fetchMock).toHaveBeenCalledTimes(1);
			});

			const [url, init] = fetchMock.mock.calls[0];
			expect(url).toBe('/api/feedback');
			expect(init.method).toBe('POST');

			const body = JSON.parse(init.body);
			expect(body.category).toBe('general');
			expect(body.body).toBe('The zoom tool is laggy');
			expect(body.context).toBeDefined();
		});

		it('includes CSRF header on submit', async () => {
			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Some feedback');

			await waitFor(() => {
				expect(fetchMock).toHaveBeenCalled();
			});

			const headers = fetchMock.mock.calls[0][1].headers;
			expect(headers['X-XSRF-TOKEN']).toBe('test-csrf-token');
			expect(headers['Content-Type']).toBe('application/json');
		});

		it('includes credentials: include', async () => {
			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Some feedback');

			await waitFor(() => {
				expect(fetchMock).toHaveBeenCalled();
			});

			expect(fetchMock.mock.calls[0][1].credentials).toBe('include');
		});

		it('captures context with route and screen info', async () => {
			page.url = new URL('http://localhost/app/case/C-2026-099') as any;
			page.params = { accession: 'C-2026-099' };

			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Context test');

			await waitFor(() => {
				expect(fetchMock).toHaveBeenCalled();
			});

			const body = JSON.parse(fetchMock.mock.calls[0][1].body);
			expect(body.context.route).toBe('/app/case/C-2026-099');
			expect(body.context.caseAccession).toBe('C-2026-099');
			expect(body.context.userAgent).toBeDefined();
			expect(body.context.screenSize).toMatch(/^\d+x\d+$/);
		});

		it('shows success message after successful submit', async () => {
			globalThis.fetch = mockFetchOk();
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Great feature');

			await waitFor(() => {
				expect(container.textContent).toContain('Captured');
			});
		});
	});

	// ── Error handling ──────────────────────────────────────────

	describe('error handling', () => {
		it('shows error message on failed submit', async () => {
			globalThis.fetch = mockFetchFail(500);
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'This will fail');

			await waitFor(() => {
				expect(container.textContent).toContain('Submit failed: 500');
			});
		});

		it('shows error on network failure', async () => {
			globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network error'));
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Network test');

			await waitFor(() => {
				expect(container.textContent).toContain('Network error');
			});
		});

		it('keeps modal open after error so user can retry', async () => {
			globalThis.fetch = mockFetchFail(500);
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, 'Retry me');

			await waitFor(() => {
				expect(container.textContent).toContain('Submit failed');
			});

			// Modal should still be open with the textarea
			expect(container.querySelector('textarea')).not.toBeNull();
			// Transcript should be preserved
			expect((container.querySelector('textarea') as HTMLTextAreaElement).value).toBe('Retry me');
		});
	});

	// ── Empty guard ─────────────────────────────────────────────

	describe('empty guard', () => {
		it('does not submit when transcript is empty', async () => {
			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			const textarea = container.querySelector('textarea')!;
			await fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });

			// Give it a tick to ensure no fetch was triggered
			await new Promise(r => setTimeout(r, 50));
			expect(fetchMock).not.toHaveBeenCalled();
		});

		it('does not submit when transcript is only whitespace', async () => {
			const fetchMock = mockFetchOk();
			globalThis.fetch = fetchMock;
			const { container } = render(Echo);
			await openEcho(container);

			await typeAndSubmit(container, '   ');

			await new Promise(r => setTimeout(r, 50));
			expect(fetchMock).not.toHaveBeenCalled();
		});
	});
});
