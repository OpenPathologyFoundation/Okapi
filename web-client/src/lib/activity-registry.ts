import type { ActivityDefinition } from '$lib/types/activity';

export const ACTIVITY_REGISTRY: Record<string, ActivityDefinition> = {
	'pelican-viewer': {
		id: 'pelican-viewer',
		label: 'Digital Viewer',
		type: 'external',
		path: '/viewer/orchestrated.html',
		caseScoped: true,
		bridge: 'viewer',
		mount: {
			strategy: 'window',
			windowFeatures: 'width=1400,height=900,menubar=no,toolbar=no,location=no,status=no',
			protocolVersion: '1.0',
		},
	},
	'report-module': {
		id: 'report-module',
		label: 'Report Authoring',
		type: 'external',
		path: '/report/orchestrated',
		caseScoped: true,
		bridge: 'report',
		mount: {
			strategy: 'iframe',
			protocolVersion: '1.0',
			slot: 'case-secondary',
		},
	},
};
