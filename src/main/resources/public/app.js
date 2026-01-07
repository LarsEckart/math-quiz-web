/**
 * Math Quiz - Audio and UI handling
 */

const audioPlayer = {
    element: null,
    currentHash: null,
    currentCallback: null,

    init() {
        this.element = document.getElementById('tts-audio');
        if (this.element) {
            this.element.addEventListener('ended', () => this.onEnded());
            this.element.addEventListener('error', () => this.onEnded());
        }
    },

    play(hash, onFinish = null) {
        if (!this.element || !hash || hash === 'null') {
            if (onFinish) onFinish();
            return;
        }

        // If already playing this exact hash, ignore (duplicate event)
        if (this.currentHash === hash) {
            return;
        }

        // If something else is playing, stop it
        if (this.currentHash) {
            this.element.pause();
            this.currentHash = null;
            this.currentCallback = null;
        }

        this.currentHash = hash;
        this.currentCallback = onFinish;
        
        this.element.src = `/audio/${hash}.wav`;
        this.element.play().catch(() => this.onEnded());
    },

    onEnded() {
        if (!this.currentHash) return;
        
        const cb = this.currentCallback;
        this.currentHash = null;
        this.currentCallback = null;
        
        if (cb) cb();
    },

    replay() {
        if (this.element && this.element.src) {
            this.element.currentTime = 0;
            this.element.play().catch(() => {});
        }
    }
};

// Track which content we've already processed to avoid duplicate HTMX events
let lastProcessedHash = null;

document.addEventListener('DOMContentLoaded', () => {
    audioPlayer.init();
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'r' || e.key === 'R') {
        audioPlayer.replay();
    }
});

document.body.addEventListener('htmx:afterSwap', (e) => {
    if (e.detail.target.id !== 'problem-area') return;
    
    const content = e.detail.target.firstElementChild;
    if (!content) return;

    const audioHash = content.dataset.audioHash;
    const isFeedback = content.classList.contains('feedback');

    // Skip duplicate events (HTMX fires multiple afterSwap for OOB swaps)
    if (audioHash && audioHash === lastProcessedHash) {
        return;
    }
    lastProcessedHash = audioHash;

    if (isFeedback) {
        audioPlayer.play(audioHash, () => {
            setTimeout(() => {
                htmx.ajax('GET', '/quiz/problem', { target: '#problem-area', swap: 'innerHTML' });
            }, 500);
        });
    } else {
        audioPlayer.play(audioHash, null);
        
        const input = document.querySelector('.answer-input');
        if (input) {
            input.focus();
        }
    }
});
