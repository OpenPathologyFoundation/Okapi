import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	server: {
		proxy: {
			'/oauth2': {
				target: 'http://localhost:8080',
				changeOrigin: false
			},
			'/login/oauth2': {
				target: 'http://localhost:8080',
				changeOrigin: false
			},
			'/auth': {
				target: 'http://localhost:8080',
				changeOrigin: false
			},
			'/admin': {
				target: 'http://localhost:8080',
				changeOrigin: false
			},
			'/logout': {
				target: 'http://localhost:8080',
				changeOrigin: false
			}
		}
	}
});
