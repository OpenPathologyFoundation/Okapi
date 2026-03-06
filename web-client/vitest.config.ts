import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { resolve } from 'path';

export default defineConfig({
	plugins: [svelte({ hot: false })],
	resolve: {
		conditions: ['browser'],
	},
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['src/**/*.test.ts'],
		setupFiles: ['src/test-mocks/setup.ts'],
		alias: {
			$lib: resolve('./src/lib'),
			'$app/environment': resolve('./src/test-mocks/app-environment.ts'),
			'$app/state': resolve('./src/test-mocks/app-state.ts'),
			'$app/navigation': resolve('./src/test-mocks/app-navigation.ts'),
		},
	},
});
