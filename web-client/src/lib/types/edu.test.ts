/**
 * Educational WSI Collection Types — Unit Tests
 *
 * Validates that all label maps, color maps, and helper arrays
 * are complete and consistent with their respective type unions.
 */

import { describe, it, expect } from 'vitest';
import {
	STATUS_LABELS,
	STATUS_COLORS,
	DIFFICULTY_LABELS,
	DIFFICULTY_COLORS,
	ROLE_LABELS,
	VISIBILITY_LABELS,
	CATEGORY_LABELS,
	ALL_STATUSES,
	ALL_DIFFICULTIES,
	ALL_CURATOR_ROLES
} from './edu';
import type {
	EduCaseStatus,
	DifficultyLevel,
	EduCuratorRole,
	EduCollectionVisibility
} from './edu';

// ── Status Labels & Colors ─────────────────────────────────

describe('STATUS_LABELS', () => {
	const expectedStatuses: EduCaseStatus[] = ['active', 'archived', 'draft'];

	it('has a label for every status', () => {
		for (const status of expectedStatuses) {
			expect(STATUS_LABELS[status]).toBeDefined();
			expect(STATUS_LABELS[status]).not.toBe('');
		}
	});

	it('has exactly the expected number of entries', () => {
		expect(Object.keys(STATUS_LABELS)).toHaveLength(expectedStatuses.length);
	});
});

describe('STATUS_COLORS', () => {
	const expectedStatuses: EduCaseStatus[] = ['active', 'archived', 'draft'];

	it('has a color for every status', () => {
		for (const status of expectedStatuses) {
			expect(STATUS_COLORS[status]).toBeDefined();
			expect(STATUS_COLORS[status]).not.toBe('');
		}
	});

	it('color strings reference edu-status CSS custom properties', () => {
		for (const status of expectedStatuses) {
			expect(STATUS_COLORS[status]).toContain(`edu-status-${status}`);
		}
	});
});

// ── Difficulty Labels & Colors ─────────────────────────────

describe('DIFFICULTY_LABELS', () => {
	const expectedLevels: DifficultyLevel[] = ['beginner', 'intermediate', 'advanced'];

	it('has a label for every difficulty level', () => {
		for (const level of expectedLevels) {
			expect(DIFFICULTY_LABELS[level]).toBeDefined();
			expect(DIFFICULTY_LABELS[level]).not.toBe('');
		}
	});

	it('has exactly the expected number of entries', () => {
		expect(Object.keys(DIFFICULTY_LABELS)).toHaveLength(expectedLevels.length);
	});

	it('labels are human-readable (capitalized)', () => {
		expect(DIFFICULTY_LABELS.beginner).toBe('Beginner');
		expect(DIFFICULTY_LABELS.intermediate).toBe('Intermediate');
		expect(DIFFICULTY_LABELS.advanced).toBe('Advanced');
	});
});

describe('DIFFICULTY_COLORS', () => {
	const expectedLevels: DifficultyLevel[] = ['beginner', 'intermediate', 'advanced'];

	it('has a color for every difficulty level', () => {
		for (const level of expectedLevels) {
			expect(DIFFICULTY_COLORS[level]).toBeDefined();
			expect(DIFFICULTY_COLORS[level]).not.toBe('');
		}
	});

	it('color strings reference edu-difficulty CSS custom properties', () => {
		for (const level of expectedLevels) {
			expect(DIFFICULTY_COLORS[level]).toContain(`edu-difficulty-${level}`);
		}
	});
});

// ── Role Labels ────────────────────────────────────────────

describe('ROLE_LABELS', () => {
	const expectedRoles: EduCuratorRole[] = ['PRIMARY_CURATOR', 'CURATOR', 'CONTRIBUTOR'];

	it('has a label for every curator role', () => {
		for (const role of expectedRoles) {
			expect(ROLE_LABELS[role]).toBeDefined();
			expect(ROLE_LABELS[role]).not.toBe('');
		}
	});

	it('has exactly the expected number of entries', () => {
		expect(Object.keys(ROLE_LABELS)).toHaveLength(expectedRoles.length);
	});

	it('labels are human-readable', () => {
		expect(ROLE_LABELS.PRIMARY_CURATOR).toBe('Primary Curator');
		expect(ROLE_LABELS.CURATOR).toBe('Curator');
		expect(ROLE_LABELS.CONTRIBUTOR).toBe('Contributor');
	});
});

// ── Visibility Labels ──────────────────────────────────────

describe('VISIBILITY_LABELS', () => {
	const expectedVisibilities: EduCollectionVisibility[] = ['PRIVATE', 'DEPARTMENT', 'INSTITUTION'];

	it('has a label for every visibility level', () => {
		for (const vis of expectedVisibilities) {
			expect(VISIBILITY_LABELS[vis]).toBeDefined();
			expect(VISIBILITY_LABELS[vis]).not.toBe('');
		}
	});

	it('has exactly the expected number of entries', () => {
		expect(Object.keys(VISIBILITY_LABELS)).toHaveLength(expectedVisibilities.length);
	});
});

// ── Category Labels ────────────────────────────────────────

describe('CATEGORY_LABELS', () => {
	it('includes the categories used in seed data', () => {
		const seedCategories = [
			'neuropathology',
			'pulmonary_pathology',
			'gi_pathology',
			'genitourinary_pathology',
			'hepatopathology',
			'endocrine_pathology',
			'breast_pathology',
			'dermatopathology',
			'hematopathology',
			'cytopathology'
		];

		for (const cat of seedCategories) {
			expect(CATEGORY_LABELS[cat]).toBeDefined();
			expect(CATEGORY_LABELS[cat]).not.toBe('');
		}
	});

	it('labels do not contain underscores', () => {
		for (const label of Object.values(CATEGORY_LABELS)) {
			expect(label).not.toContain('_');
		}
	});
});

// ── Helper Arrays ──────────────────────────────────────────

describe('ALL_STATUSES', () => {
	it('contains all status values', () => {
		expect(ALL_STATUSES).toContain('active');
		expect(ALL_STATUSES).toContain('archived');
		expect(ALL_STATUSES).toContain('draft');
	});

	it('has exactly 3 entries', () => {
		expect(ALL_STATUSES).toHaveLength(3);
	});
});

describe('ALL_DIFFICULTIES', () => {
	it('contains all difficulty levels', () => {
		expect(ALL_DIFFICULTIES).toContain('beginner');
		expect(ALL_DIFFICULTIES).toContain('intermediate');
		expect(ALL_DIFFICULTIES).toContain('advanced');
	});

	it('has exactly 3 entries', () => {
		expect(ALL_DIFFICULTIES).toHaveLength(3);
	});
});

describe('ALL_CURATOR_ROLES', () => {
	it('contains all curator roles', () => {
		expect(ALL_CURATOR_ROLES).toContain('PRIMARY_CURATOR');
		expect(ALL_CURATOR_ROLES).toContain('CURATOR');
		expect(ALL_CURATOR_ROLES).toContain('CONTRIBUTOR');
	});

	it('has exactly 3 entries', () => {
		expect(ALL_CURATOR_ROLES).toHaveLength(3);
	});
});

// ── Cross-checks ───────────────────────────────────────────

describe('Label/color map consistency', () => {
	it('STATUS_LABELS and STATUS_COLORS have the same keys', () => {
		expect(Object.keys(STATUS_LABELS).sort()).toEqual(Object.keys(STATUS_COLORS).sort());
	});

	it('DIFFICULTY_LABELS and DIFFICULTY_COLORS have the same keys', () => {
		expect(Object.keys(DIFFICULTY_LABELS).sort()).toEqual(Object.keys(DIFFICULTY_COLORS).sort());
	});

	it('ALL_STATUSES matches STATUS_LABELS keys', () => {
		expect([...ALL_STATUSES].sort()).toEqual(Object.keys(STATUS_LABELS).sort());
	});

	it('ALL_DIFFICULTIES matches DIFFICULTY_LABELS keys', () => {
		expect([...ALL_DIFFICULTIES].sort()).toEqual(Object.keys(DIFFICULTY_LABELS).sort());
	});

	it('ALL_CURATOR_ROLES matches ROLE_LABELS keys', () => {
		expect([...ALL_CURATOR_ROLES].sort()).toEqual(Object.keys(ROLE_LABELS).sort());
	});
});
