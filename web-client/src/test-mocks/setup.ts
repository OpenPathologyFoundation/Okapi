import '@testing-library/jest-dom/vitest';

// Polyfill Element.animate for Svelte transitions in jsdom
if (typeof Element.prototype.animate !== 'function') {
	Element.prototype.animate = function (_keyframes: Keyframe[] | PropertyIndexedKeyframes | null, _options?: number | KeyframeAnimationOptions) {
		return {
			onfinish: null as (() => void) | null,
			cancel: () => {},
			finish: function (this: { onfinish: (() => void) | null }) {
				if (this.onfinish) this.onfinish();
			},
			play: () => {},
			pause: () => {},
			reverse: () => {},
			persist: () => {},
			commitStyles: () => {},
			addEventListener: () => {},
			removeEventListener: () => {},
			dispatchEvent: () => true,
			get finished() { return Promise.resolve(this); },
			get ready() { return Promise.resolve(this); },
			// Trigger onfinish immediately so transitions complete synchronously
			...((() => {
				const anim = {} as any;
				queueMicrotask(() => { if (anim.onfinish) anim.onfinish(); });
				return anim;
			})()),
		} as unknown as Animation;
	};
}
