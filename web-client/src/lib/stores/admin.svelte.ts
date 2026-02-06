import { browser } from '$app/environment';
import type {
	IdentitySummary,
	IdentityDetail,
	RoleSummary,
	RolePermissionRow,
	IdpMappingSummary,
	AuditEventSummary,
	BreakGlassGrantSummary,
	ResearchGrantSummary,
	DeviceSummary,
	DashboardSummary,
	PageResponse
} from '$lib/types/admin';

class AdminStore {
	// ── State ──────────────────────────────────────────────────────
	identities = $state<IdentitySummary[]>([]);
	identityPage = $state(0);
	identityTotalPages = $state(0);
	identityTotalElements = $state(0);

	selectedIdentity = $state<IdentityDetail | null>(null);

	roles = $state<RoleSummary[]>([]);
	rolePermissionMatrix = $state<RolePermissionRow[]>([]);

	idpMappings = $state<IdpMappingSummary[]>([]);

	auditEvents = $state<AuditEventSummary[]>([]);
	auditPage = $state(0);
	auditTotalPages = $state(0);
	auditTotalElements = $state(0);

	breakGlassGrants = $state<BreakGlassGrantSummary[]>([]);
	researchGrants = $state<ResearchGrantSummary[]>([]);

	devices = $state<DeviceSummary[]>([]);

	dashboard = $state<DashboardSummary | null>(null);

	sessionTimeoutMinutes = $state(15);

	isLoading = $state(false);
	error = $state<string | null>(null);

	// ── API Helpers ────────────────────────────────────────────────

	private async fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
		const res = await fetch(url, { credentials: 'include', ...init });
		if (!res.ok) {
			const body = await res.json().catch(() => ({}));
			throw new Error(body.message || `Request failed: ${res.status}`);
		}
		if (res.status === 204) return undefined as unknown as T;
		return res.json();
	}

	private async withLoading<T>(fn: () => Promise<T>): Promise<T | undefined> {
		if (!browser) return undefined;
		this.isLoading = true;
		this.error = null;
		try {
			return await fn();
		} catch (e) {
			this.error = e instanceof Error ? e.message : 'An error occurred';
			return undefined;
		} finally {
			this.isLoading = false;
		}
	}

	// ── Identities ─────────────────────────────────────────────────

	async loadIdentities(search?: string, active?: boolean, page = 0, size = 20): Promise<void> {
		await this.withLoading(async () => {
			const params = new URLSearchParams({ page: String(page), size: String(size) });
			if (search) params.set('search', search);
			if (active !== undefined) params.set('active', String(active));
			const data = await this.fetchJson<PageResponse<IdentitySummary>>(
				`/admin/identities?${params}`
			);
			this.identities = data.content;
			this.identityPage = data.page;
			this.identityTotalPages = data.totalPages;
			this.identityTotalElements = data.totalElements;
		});
	}

	async loadIdentity(id: string): Promise<void> {
		await this.withLoading(async () => {
			this.selectedIdentity = await this.fetchJson<IdentityDetail>(
				`/admin/identities/${id}`
			);
		});
	}

	async setIdentityActive(id: string, active: boolean): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/identities/${id}/status`, {
				method: 'PATCH',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ active })
			});
			if (this.selectedIdentity?.identityId === id) {
				this.selectedIdentity = { ...this.selectedIdentity, isActive: active };
			}
		});
	}

	async assignRole(identityId: string, roleId: string, justification?: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/identities/${identityId}/roles`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ roleId, justification: justification ?? '' })
			});
		});
	}

	async revokeRole(identityId: string, roleId: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/identities/${identityId}/roles/${roleId}`, {
				method: 'DELETE'
			});
		});
	}

	// ── Roles ──────────────────────────────────────────────────────

	async loadRoles(): Promise<void> {
		await this.withLoading(async () => {
			this.roles = await this.fetchJson<RoleSummary[]>('/admin/roles');
		});
	}

	async loadRolePermissionMatrix(): Promise<void> {
		await this.withLoading(async () => {
			this.rolePermissionMatrix = await this.fetchJson<RolePermissionRow[]>(
				'/admin/roles/permissions'
			);
		});
	}

	// ── IdP Mappings ───────────────────────────────────────────────

	async loadIdpMappings(): Promise<void> {
		await this.withLoading(async () => {
			this.idpMappings = await this.fetchJson<IdpMappingSummary[]>('/admin/idp-mappings');
		});
	}

	async createIdpMapping(
		providerId: string,
		groupName: string,
		description: string,
		roleIds: string[]
	): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson('/admin/idp-mappings', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ providerId, groupName, description, roleIds })
			});
			await this.loadIdpMappings();
		});
	}

	async deleteIdpMapping(id: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/idp-mappings/${id}`, { method: 'DELETE' });
			this.idpMappings = this.idpMappings.filter((m) => m.idpGroupId !== id);
		});
	}

	// ── Audit Events ───────────────────────────────────────────────

	async loadAuditEvents(
		filters: {
			eventType?: string;
			actorId?: string;
			targetType?: string;
			outcome?: string;
			from?: string;
			to?: string;
		} = {},
		page = 0,
		size = 20
	): Promise<void> {
		await this.withLoading(async () => {
			const params = new URLSearchParams({ page: String(page), size: String(size) });
			if (filters.eventType) params.set('eventType', filters.eventType);
			if (filters.actorId) params.set('actorId', filters.actorId);
			if (filters.targetType) params.set('targetType', filters.targetType);
			if (filters.outcome) params.set('outcome', filters.outcome);
			if (filters.from) params.set('from', filters.from);
			if (filters.to) params.set('to', filters.to);
			const data = await this.fetchJson<PageResponse<AuditEventSummary>>(
				`/admin/audit-events?${params}`
			);
			this.auditEvents = data.content;
			this.auditPage = data.page;
			this.auditTotalPages = data.totalPages;
			this.auditTotalElements = data.totalElements;
		});
	}

	// ── Grants ─────────────────────────────────────────────────────

	async loadBreakGlassGrants(): Promise<void> {
		await this.withLoading(async () => {
			this.breakGlassGrants = await this.fetchJson<BreakGlassGrantSummary[]>(
				'/admin/grants/break-glass'
			);
		});
	}

	async revokeBreakGlassGrant(id: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/grants/break-glass/${id}`, { method: 'DELETE' });
			this.breakGlassGrants = this.breakGlassGrants.filter((g) => g.grantId !== id);
		});
	}

	async loadResearchGrants(): Promise<void> {
		await this.withLoading(async () => {
			this.researchGrants = await this.fetchJson<ResearchGrantSummary[]>(
				'/admin/grants/research'
			);
		});
	}

	async revokeResearchGrant(id: string, reason?: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/grants/research/${id}`, {
				method: 'DELETE',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ reason: reason ?? '' })
			});
			this.researchGrants = this.researchGrants.filter((g) => g.grantId !== id);
		});
	}

	// ── Devices ────────────────────────────────────────────────────

	async loadDevices(identityId: string): Promise<void> {
		await this.withLoading(async () => {
			this.devices = await this.fetchJson<DeviceSummary[]>(
				`/admin/devices?identityId=${identityId}`
			);
		});
	}

	async revokeDevice(id: string): Promise<void> {
		await this.withLoading(async () => {
			await this.fetchJson(`/admin/devices/${id}`, { method: 'DELETE' });
			this.devices = this.devices.map((d) =>
				d.deviceId === id ? { ...d, revokedAt: new Date().toISOString() } : d
			);
		});
	}

	// ── Dashboard ──────────────────────────────────────────────────

	async loadDashboard(): Promise<void> {
		await this.withLoading(async () => {
			this.dashboard = await this.fetchJson<DashboardSummary>('/admin/dashboard');
		});
	}

	// ── Settings ────────────────────────────────────────────────────

	async loadSessionTimeout(): Promise<void> {
		await this.withLoading(async () => {
			const data = await this.fetchJson<{ timeoutMinutes: number }>(
				'/admin/settings/session-timeout'
			);
			this.sessionTimeoutMinutes = data.timeoutMinutes;
		});
	}

	async updateSessionTimeout(minutes: number): Promise<void> {
		await this.withLoading(async () => {
			const data = await this.fetchJson<{ timeoutMinutes: number }>(
				'/admin/settings/session-timeout',
				{
					method: 'PUT',
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify({ timeoutMinutes: minutes })
				}
			);
			this.sessionTimeoutMinutes = data.timeoutMinutes;
		});
	}
}

export const adminStore = new AdminStore();
