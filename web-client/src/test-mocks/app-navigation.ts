/** Mock for $app/navigation */
export function goto(_url: string, _opts?: unknown) {
	return Promise.resolve();
}

export function invalidate(_url: string) {
	return Promise.resolve();
}

export function invalidateAll() {
	return Promise.resolve();
}

export function preloadData(_url: string) {
	return Promise.resolve();
}

export function preloadCode(..._urls: string[]) {
	return Promise.resolve();
}

export function beforeNavigate(_fn: unknown) {}
export function afterNavigate(_fn: unknown) {}
export function onNavigate(_fn: unknown) {}
