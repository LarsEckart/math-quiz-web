@import mathquiz.domain.Operation

@param boolean correct
@param int correctAnswer
@param int streak
@param int todayStars
@param int totalStars
@param boolean rangeExpanded
@param Operation newOperationUnlocked
@param int newStars

<div class="feedback ${correct ? "correct" : "incorrect"}">
    @if(correct)
        <div class="feedback-icon">✅</div>
        <div class="feedback-text">Õige!</div>
    @else
        <div class="feedback-icon">❌</div>
        <div class="feedback-text">Vale! Õige vastus: ${correctAnswer}</div>
    @endif

    @if(newStars > 0)
        <div class="star-earned">⭐</div>
    @endif

    @if(rangeExpanded)
        <div class="achievement">🎯 Raskusaste tõusis!</div>
    @endif

    @if(newOperationUnlocked != null)
        <div class="achievement">🔓 Uus tehe avatud: ${newOperationUnlocked.symbol()}</div>
    @endif

    <%-- Auto-advance to next problem --%>
    <div hx-get="/quiz/problem"
         hx-trigger="load delay:1200ms"
         hx-target="#problem-area"
         hx-swap="innerHTML">
    </div>
</div>

<%-- OOB updates for stats --%>
<span id="streak" class="stat streak" hx-swap-oob="true">🔥 ${streak}</span>
<span id="today-stars" class="stat stars" hx-swap-oob="true">⭐ ${todayStars}</span>
<span id="total-stars" class="stat total-stars" hx-swap-oob="true">🌟 ${totalStars}</span>
