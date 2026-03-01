/**
 * CSRF Token Utilities
 *
 * Spring Security sets an XSRF-TOKEN cookie (httpOnly: false) on every
 * response when CookieCsrfTokenRepository is configured. JavaScript must
 * read this cookie and send it back as the X-XSRF-TOKEN header on any
 * state-changing request (POST, PUT, DELETE, PATCH).
 */

/** Read the XSRF-TOKEN cookie value, or null if not set */
export function getCsrfToken(): string | null {
	const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
	return match ? decodeURIComponent(match[1]) : null;
}

/** Return headers object with the X-XSRF-TOKEN header if the cookie exists */
export function csrfHeaders(): Record<string, string> {
	const token = getCsrfToken();
	return token ? { 'X-XSRF-TOKEN': token } : {};
}
