import { browser } from '$app/environment';

class IdleStore {
	idleTimeoutMs = $state(15 * 60 * 1000);
	lastActivityAt = $state(Date.now());
	warningShown = $state(false);
	secondsRemaining = $state(0);

	private checkInterval: ReturnType<typeof setInterval> | null = null;
	private throttleTimer: ReturnType<typeof setTimeout> | null = null;
	private boundOnActivity: (() => void) | null = null;
	private initialized = false;

	init(timeoutMinutes: number): void {
		if (!browser || this.initialized) return;
		this.initialized = true;
		this.idleTimeoutMs = timeoutMinutes * 60 * 1000;
		this.lastActivityAt = Date.now();
		this.warningShown = false;
		this.secondsRemaining = 0;

		this.boundOnActivity = this.onActivity.bind(this);
		const opts: AddEventListenerOptions = { passive: true };
		for (const event of ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']) {
			window.addEventListener(event, this.boundOnActivity, opts);
		}

		this.checkInterval = setInterval(() => this.tick(), 1000);
	}

	private onActivity(): void {
		if (this.throttleTimer) return;
		this.lastActivityAt = Date.now();
		if (this.warningShown) {
			this.warningShown = false;
			this.secondsRemaining = 0;
		}
		this.throttleTimer = setTimeout(() => {
			this.throttleTimer = null;
		}, 30_000);
	}

	private tick(): void {
		const elapsed = Date.now() - this.lastActivityAt;
		const warningThreshold = this.idleTimeoutMs - 60_000;

		if (elapsed >= this.idleTimeoutMs) {
			this.destroy();
			window.location.href = '/auth/logout';
			return;
		}

		if (elapsed >= warningThreshold) {
			this.warningShown = true;
			this.secondsRemaining = Math.ceil((this.idleTimeoutMs - elapsed) / 1000);
		}
	}

	resetTimer(): void {
		this.lastActivityAt = Date.now();
		this.warningShown = false;
		this.secondsRemaining = 0;
	}

	destroy(): void {
		if (!browser) return;
		if (this.checkInterval) {
			clearInterval(this.checkInterval);
			this.checkInterval = null;
		}
		if (this.throttleTimer) {
			clearTimeout(this.throttleTimer);
			this.throttleTimer = null;
		}
		if (this.boundOnActivity) {
			for (const event of ['mousemove', 'keydown', 'click', 'scroll', 'touchstart']) {
				window.removeEventListener(event, this.boundOnActivity);
			}
			this.boundOnActivity = null;
		}
		this.initialized = false;
	}
}

export const idleStore = new IdleStore();
