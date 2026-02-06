export interface AuthUser {
	identityId: string;
	providerId: string;
	externalSubject: string;
	email: string;
	displayName: string;
	displayShort: string;
	givenName: string;
	familyName: string;
	middleName: string | null;
	middleInitial: string | null;
	prefix: string | null;
	suffix: string | null;
	roles: string[];
	permissions: string[];
	isAdmin: boolean;
	authorities: string[];
}
