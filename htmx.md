# HTMX Gotchas & Patterns

Lessons learned from building this app with HTMX.

## OOB Swaps Trigger Multiple `afterSwap` Events

**Problem:** When a response contains out-of-band (OOB) swaps, HTMX fires `htmx:afterSwap` multiple times for the *main target* - once for the primary swap and once for each OOB element.

**Example:** Our feedback response:
```html
<div class="feedback">...</div>

<!-- OOB updates -->
<span id="streak" hx-swap-oob="true">🔥 5</span>
<span id="today-stars" hx-swap-oob="true">⭐ 3</span>
<span id="total-stars" hx-swap-oob="true">🌟 10</span>
```

This fires `afterSwap` for `#problem-area` **4 times** (1 main + 3 OOB).

**Solution:** Track processed content and deduplicate:
```javascript
let lastProcessedHash = null;

document.body.addEventListener('htmx:afterSwap', (e) => {
    if (e.detail.target.id !== 'problem-area') return;
    
    const content = e.detail.target.firstElementChild;
    const contentId = content?.dataset.someUniqueId;
    
    // Skip duplicate events
    if (contentId && contentId === lastProcessedHash) {
        return;
    }
    lastProcessedHash = contentId;
    
    // ... handle the swap
});
```

## Audio Elements Get Destroyed on Swap

**Problem:** If you put `<audio>` inside a container that HTMX swaps, the audio stops playing when the swap happens.

**Solution:** Place persistent audio elements outside the swap target:
```html
<body>
    <main>
        <div id="problem-area">
            <!-- HTMX swaps content here -->
        </div>
    </main>
    
    <!-- Audio player lives here, never gets swapped -->
    <audio id="tts-audio"></audio>
</body>
```

Pass audio info via data attributes:
```html
<div class="problem" data-audio-hash="abc123">
```

Then handle playback in JS by reading the data attribute after swap.

## Auto-Advance Pattern (Wait for Audio)

**Problem:** Using `hx-trigger="load delay:2000ms"` for auto-advance doesn't account for variable audio length.

**Solution:** Handle auto-advance in JS after audio finishes:
```javascript
if (isFeedback) {
    audioPlayer.play(audioHash, () => {
        // Callback fires when audio ends
        setTimeout(() => {
            htmx.ajax('GET', '/next', { target: '#content', swap: 'innerHTML' });
        }, 500);  // Small pause after audio
    });
}
```

## JTE: No `@if` in HTML Attribute Names

**Problem:** JTE doesn't allow `@if` expressions in attribute names:
```html
<!-- This fails! -->
<div class="foo"@if(bar != null) data-bar="${bar}"@endif>
```

**Solution:** Use JTE's smart attributes - null values omit the attribute automatically:
```html
<!-- This works - attribute omitted if bar is null -->
<div class="foo" data-bar="${bar}">
```

In JS, handle the literal string "null":
```javascript
if (!hash || hash === 'null') return;
```
