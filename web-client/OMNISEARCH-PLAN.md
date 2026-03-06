Excellent. Can you double check if we have enough unit tests for the UI here just to check that it works and works flawlessly? And if anything needs to be added to the test case set so that we can run and validate the coverage?# Context-Aware OmniSearch

## Context

The OmniSearch bar in the top nav currently only searches clinical worklist cases (`/api/worklist`). When a user is on the education page and types "EDU26", it hits the worklist API and returns nothing. The same problem exists on admin pages — searching for a person's name searches cases, not identities. The search bar should adapt to the user's current context.

## Approach

Make OmniSearch **route-aware** using SvelteKit's `page` store. Define a `SearchContext` configuration per route group that controls: placeholder text, API endpoint, result rendering, and navigation behavior.

## Search Contexts

| Route Pattern | Placeholder | API | Result Fields | Click → | View All → |
|---|---|---|---|---|---|
| `/app/edu/*` | "Search educational cases by ID, diagnosis, or organ..." | `GET /api/edu/cases?query={term}&pageSize=8` | caseId, primaryDiagnosis, anatomicSite, difficulty badge, status badge | `/app/edu/case/{caseId}` | `/app/edu` with query filter |
| `/app/admin/*` | "Search identities by name, email, or username..." | `GET /api/identities/search?q={term}&limit=8` | displayName, email, role badges | `/app/admin/identities` (filtered) | `/app/admin/identities` with search |
| Everything else | "Search cases by accession, patient, or MRN..." (current) | `GET /api/worklist?search={term}&pageSize=8` | accessionNumber, patientDisplay, patientMrn, service/status badges | `/app/case/{accessionNumber}` | `/app/worklist` with search |

## Files to Create

### `src/lib/types/search-context.ts` (new)
- `SearchContextType` union: `'worklist' | 'edu' | 'admin'`
- `SearchContext` interface: `{ type, placeholder, search(term): Promise<SearchResult[]>, totalItems, navigateTo(result), viewAll(term) }`
- `SearchResult` discriminated union with `type` field — each variant carries its own fields (worklist item, edu case, identity)

### `src/lib/utils/search-contexts.ts` (new)
- `getSearchContext(pathname: string): SearchContext` — pattern-matches pathname to return the appropriate context
- Three context factory functions, each encapsulating:
  - The fetch call with correct endpoint/parameter names (`search` vs `query` vs `q`)
  - Response normalization into `SearchResult[]`
  - `navigateTo()` using `goto()`
  - `viewAll()` using store mutations + `goto()`

## Files to Modify

### `src/lib/components/OmniSearch.svelte`
- Import `page` from `$app/state` and `getSearchContext`
- Add `context = $derived(getSearchContext(page.url.pathname))`
- Replace hardcoded `searchCases()` with `context.search(term)`
- Replace hardcoded placeholder with `{context.placeholder}`
- Clear results when context changes (route navigation)
- Conditional result rendering using `{#if context.type === 'worklist'}` / `'edu'` / `'admin'` blocks — each with appropriate fields and badges
- Update `navigateToCase()` → generic `navigateToResult(result)`
- Update `viewAllResults()` → `context.viewAll(query)`
- Update footer text: "View all {totalItems} results in {contextLabel}"

### `src/routes/app/edu/+page.svelte`
- Remove the search `<input>` from the sidebar filter panel (lines ~64-74, the "Case ID, diagnosis, organ..." input)
- Keep all other filters (anatomic site, difficulty, stain) intact

## Implementation Order

1. Create `search-context.ts` types
2. Create `search-contexts.ts` utilities
3. Refactor `OmniSearch.svelte` — context detection, search dispatch, conditional rendering
4. Remove edu sidebar search input
5. Rebuild web client on server (`npm run build`)
6. Verify all three contexts work end-to-end

## Verification

- Navigate to `/app/worklist`, type "S26" — should show clinical cases, click navigates to case viewer
- Navigate to `/app/edu`, type "EDU26" — should show edu cases with diagnosis/organ, click navigates to edu case
- Navigate to `/app/admin`, type a name — should show identities with email/roles, click navigates to identity detail
- "View all" in each context navigates to the correct list page with search pre-filled
- Keyboard navigation (arrows, Enter, Escape) works in all contexts
- Placeholder text changes as you navigate between sections
