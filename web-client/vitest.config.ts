import { defineConfig } from 'vitest/config';
import { resolve } from 'path';

export default defineConfig({
	test: {
		environment: 'jsdom',
		globals: true,
		include: ['src/**/*.test.ts'],
		alias: {
			$lib: resolve('./src/lib'),
			'$app/environment': resolve('./src/test-mocks/app-environment.ts'),
		},
	},
});
