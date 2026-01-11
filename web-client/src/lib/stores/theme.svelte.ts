import { browser } from '$app/environment';

type Theme = 'system' | 'dark' | 'light';

class ThemeStore {
    mode = $state<Theme>('system');

    constructor() {
        if (browser) {
            // Read from local storage
            const stored = localStorage.getItem('theme_preference') as Theme | null;
            if (stored) {
                this.mode = stored;
            } else {
                this.mode = 'system';
            }
            this.apply();
        }
    }

    setMode(newMode: Theme) {
        this.mode = newMode;
        if (browser) {
            localStorage.setItem('theme_preference', newMode);
            this.apply();
        }
    }

    private apply() {
        if (!browser) return;

        const root = document.documentElement;
        const isDark =
            this.mode === 'dark' ||
            (this.mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

        // Toggle .dark class
        if (isDark) {
            root.classList.add('dark');
        } else {
            root.classList.remove('dark');
        }
    }

    // Initialize listener for system changes
    init() {
        if (browser) {
            const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
            mediaQuery.addEventListener('change', () => {
                if (this.mode === 'system') {
                    this.apply();
                }
            });
            // Initial apply
            this.apply();
        }
    }
}

export const themeStore = new ThemeStore();
