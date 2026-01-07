/**
 * Math Quiz - Minimal JavaScript
 * Only handles audio repeat functionality
 */

// Repeat audio on 'r' key press
document.addEventListener('keydown', (e) => {
    if (e.key === 'r' || e.key === 'R') {
        const audio = document.getElementById('tts-audio');
        if (audio) {
            audio.currentTime = 0;
            audio.play();
        }
    }
});

// Auto-focus answer input when new problem loads
document.body.addEventListener('htmx:afterSwap', (e) => {
    if (e.detail.target.id === 'problem-area') {
        const input = document.querySelector('.answer-input');
        if (input) {
            input.focus();
        }
    }
});
