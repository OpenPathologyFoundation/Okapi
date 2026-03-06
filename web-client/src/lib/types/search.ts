// ============================================
// OmniSearch Uniform Result Types
// ============================================

export type SearchContext = 'worklist' | 'edu';

export interface SearchResult {
	id: string;
	title: string;
	subtitle: string;
	badge1: { label: string; color: string } | null;
	badge2: { label: string; color: string } | null;
	href: string;
}

export interface SearchResultSet {
	items: SearchResult[];
	totalItems: number;
}
