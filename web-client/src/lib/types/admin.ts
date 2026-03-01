export interface IdentitySummary {
	identityId: string;
	displayName: string;
	displayShort: string;
	email: string;
	username: string;
	isActive: boolean;
	roles: string[];
	lastSeenAt: string | null;
	createdAt: string;
}

export interface IdentityDetail {
	identityId: string;
	providerId: string;
	externalSubject: string;
	displayName: string;
	displayShort: string;
	givenName: string;
	familyName: string;
	middleName: string | null;
	prefix: string | null;
	suffix: string | null;
	email: string;
	username: string;
	isActive: boolean;
	roles: RoleSummary[];
	attributes: Record<string, unknown>;
	lastSeenAt: string | null;
	createdAt: string;
	updatedAt: string;
}

export interface RoleSummary {
	roleId: string;
	name: string;
	description: string;
	isSystem: boolean;
}

export interface RolePermissionRow {
	roleId: string;
	roleName: string;
	roleDescription: string;
	isSystem: boolean;
	permissions: string[];
}

export interface IdpMappingSummary {
	idpGroupId: string;
	providerId: string;
	groupName: string;
	description: string;
	roleNames: string[];
}

export interface AuditEventSummary {
	eventId: string;
	occurredAt: string;
	eventType: string;
	actorIdentityId: string | null;
	actorProviderId: string | null;
	actorExternalSubject: string | null;
	targetEntityType: string | null;
	targetEntityId: string | null;
	targetIdentityId: string | null;
	outcome: string;
	outcomeReason: string | null;
	details: string | null;
	metadata: Record<string, unknown>;
}

export interface BreakGlassGrantSummary {
	grantId: string;
	identityId: string;
	scopeEntityType: string;
	scopeEntityId: string;
	reasonCode: string;
	justification: string;
	grantedAt: string;
	expiresAt: string;
}

export interface ResearchGrantSummary {
	grantId: string;
	identityId: string;
	scopeType: string;
	scopeEntityId: string | null;
	protocolId: string;
	reason: string;
	phiAccessLevel: string;
	grantedAt: string;
	expiresAt: string;
}

export interface DeviceSummary {
	deviceId: string;
	identityId: string;
	deviceFingerprintHash: string;
	firstSeenAt: string;
	lastSeenAt: string | null;
	trustedUntil: string | null;
	revokedAt: string | null;
	metadata: Record<string, unknown>;
}

export interface DashboardSummary {
	totalIdentities: number;
	activeIdentities: number;
	totalRoles: number;
	activeDevices: number;
	activeBreakGlassGrants: number;
	activeResearchGrants: number;
}

export interface PageResponse<T> {
	content: T[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}
