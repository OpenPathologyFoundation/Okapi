// ============================================
// Educational WSI Collection TypeScript Types
// Trace: qms/dhf/04-SDS/SDS-EDU-001
// ============================================

// ============================================
// ENUMS AND TYPES
// ============================================

/** Educational case status */
export type EduCaseStatus = 'active' | 'archived' | 'draft';

/** Curator role */
export type EduCuratorRole = 'PRIMARY_CURATOR' | 'CURATOR' | 'CONTRIBUTOR';

/** Named collection visibility */
export type EduCollectionVisibility = 'PRIVATE' | 'DEPARTMENT' | 'INSTITUTION';

/** Teaching difficulty level */
export type DifficultyLevel = 'beginner' | 'intermediate' | 'advanced';

// ============================================
// DATA INTERFACES
// ============================================

/** Case summary for browse/list view */
export interface EduCaseListItem {
	id: string;
	caseId: string;
	status: EduCaseStatus;
	specimenType: string | null;
	anatomicSite: string | null;
	primaryDiagnosis: string | null;
	primaryCuratorDisplay: string | null;
	slideCount: number;
	teachingCategory: string | null;
	difficultyLevel: DifficultyLevel | null;
	curriculumTags: string[] | null;
}

/** Full case detail */
export interface EduCaseDetail extends EduCaseListItem {
	clinicalHistory: string | null;
	sourceLineage: Record<string, unknown> | null;
	metadata: Record<string, unknown> | null;
	parts: EduPartDetail[];
	icdCodes: EduIcdCode[];
	curators: EduCurator[];
}

export interface EduPartDetail {
	id: string;
	partLabel: string;
	partDesignator: string | null;
	anatomicSite: string | null;
	finalDiagnosis: string | null;
	provenance: string;
	blocks: EduBlockDetail[];
}

export interface EduBlockDetail {
	id: string;
	blockLabel: string;
	blockDescription: string | null;
	provenance: string;
	slides: EduSlideDetail[];
}

export interface EduSlideDetail {
	id: string;
	slideId: string;
	relativePath: string;
	format: string;
	stain: string | null;
	levelLabel: string | null;
}

export interface EduIcdCode {
	icdCode: string;
	codeSystem: string;
	codeDescription: string | null;
}

/** Curator assignment */
export interface EduCurator {
	id: string;
	caseId: string;
	caseAccession: string;
	identityId: string;
	identityDisplay: string | null;
	role: EduCuratorRole;
	assignedAt: string;
}

/** Named collection summary */
export interface EduNamedCollection {
	id: string;
	name: string;
	description: string | null;
	ownerId: string;
	ownerDisplay: string | null;
	visibility: EduCollectionVisibility;
	caseCount: number;
	createdAt: string;
}

/** Named collection with member cases */
export interface EduNamedCollectionDetail {
	id: string;
	name: string;
	description: string | null;
	ownerDisplay: string | null;
	visibility: EduCollectionVisibility;
	cases: EduCaseListItem[];
	createdAt: string;
}

// ============================================
// SEARCH AND PAGINATION
// ============================================

/** Search/filter parameters */
export interface EduSearchFilters {
	query: string;
	anatomicSite: string;
	specimenType: string;
	difficulty: string;
	stain: string;
	curatorId: string;
	collectionId: string;
}

/** Available facet values */
export interface EduFacets {
	byAnatomicSite: Record<string, number>;
	bySpecimenType: Record<string, number>;
	byDifficulty: Record<string, number>;
	byStain: Record<string, number>;
}

/** Paginated response */
export interface EduPageResponse {
	items: EduCaseListItem[];
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
	facets: EduFacets | null;
}

// ============================================
// DISPLAY LABELS
// ============================================

export const STATUS_LABELS: Record<EduCaseStatus, string> = {
	active: 'Active',
	archived: 'Archived',
	draft: 'Draft'
};

export const STATUS_COLORS: Record<EduCaseStatus, string> = {
	active: 'bg-edu-status-active/15 text-edu-status-active border-edu-status-active/30',
	archived: 'bg-edu-status-archived/15 text-edu-status-archived border-edu-status-archived/30',
	draft: 'bg-edu-status-draft/15 text-edu-status-draft border-edu-status-draft/30'
};

export const DIFFICULTY_LABELS: Record<DifficultyLevel, string> = {
	beginner: 'Beginner',
	intermediate: 'Intermediate',
	advanced: 'Advanced'
};

export const DIFFICULTY_COLORS: Record<DifficultyLevel, string> = {
	beginner:
		'bg-edu-difficulty-beginner/15 text-edu-difficulty-beginner border-edu-difficulty-beginner/30',
	intermediate:
		'bg-edu-difficulty-intermediate/15 text-edu-difficulty-intermediate border-edu-difficulty-intermediate/30',
	advanced:
		'bg-edu-difficulty-advanced/15 text-edu-difficulty-advanced border-edu-difficulty-advanced/30'
};

export const ROLE_LABELS: Record<EduCuratorRole, string> = {
	PRIMARY_CURATOR: 'Primary Curator',
	CURATOR: 'Curator',
	CONTRIBUTOR: 'Contributor'
};

export const VISIBILITY_LABELS: Record<EduCollectionVisibility, string> = {
	PRIVATE: 'Private',
	DEPARTMENT: 'Department',
	INSTITUTION: 'Institution'
};

/** Category display labels */
export const CATEGORY_LABELS: Record<string, string> = {
	neuropathology: 'Neuropathology',
	pulmonary_pathology: 'Pulmonary Pathology',
	gi_pathology: 'GI Pathology',
	genitourinary_pathology: 'GU Pathology',
	hepatopathology: 'Hepatopathology',
	endocrine_pathology: 'Endocrine Pathology',
	breast_pathology: 'Breast Pathology',
	dermatopathology: 'Dermatopathology',
	hematopathology: 'Hematopathology',
	cytopathology: 'Cytopathology'
};

// ============================================
// HELPER ARRAYS
// ============================================

export const ALL_STATUSES: EduCaseStatus[] = ['active', 'archived', 'draft'];
export const ALL_DIFFICULTIES: DifficultyLevel[] = ['beginner', 'intermediate', 'advanced'];
export const ALL_CURATOR_ROLES: EduCuratorRole[] = [
	'PRIMARY_CURATOR',
	'CURATOR',
	'CONTRIBUTOR'
];
