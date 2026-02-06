import { browser } from '$app/environment';
import type { AuthUser } from '$lib/types/auth';

class AuthStore {
	user = $state<AuthUser | null>(null);
	isLoading = $state(true);
	error = $state<string | null>(null);

	isAdmin = $derived(this.user?.isAdmin ?? false);
	roles = $derived(this.user?.roles ?? []);
	permissions = $derived(this.user?.permissions ?? []);
	isAuthenticated = $derived(this.user !== null);

	async load(): Promise<void> {
		if (!browser) return;
		this.isLoading = true;
		this.error = null;

		try {
			const res = await fetch('/auth/me', { credentials: 'include' });
			if (!res.ok) {
				if (res.status === 401 || res.status === 403) {
					this.user = null;
					return;
				}
				throw new Error(`Failed to load user: ${res.status}`);
			}
			this.user = await res.json();
		} catch (e) {
			this.error = e instanceof Error ? e.message : 'Failed to load user';
			this.user = null;
		} finally {
			this.isLoading = false;
		}
	}

	hasPermission(permission: string): boolean {
		return this.permissions.includes(permission);
	}

	hasRole(role: string): boolean {
		return this.roles.includes(role);
	}
}

export const authStore = new AuthStore();
