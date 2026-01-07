@param String playerName
@param int streak
@param int todayStars
@param int totalStars

@template.layout(title = "Math Quiz", content = @`
    <header class="quiz-header">
        <div class="player-info">
            <span class="player-name">${playerName}</span>
            <a href="/players" class="switch-player">Vaheta</a>
        </div>
        <div class="stats-bar">
            <span id="streak" class="stat streak">🔥 ${streak}</span>
            <span id="today-stars" class="stat stars">⭐ ${todayStars}</span>
            <span id="total-stars" class="stat total-stars">🌟 ${totalStars}</span>
        </div>
    </header>

    <div id="problem-area"
         hx-get="/quiz/problem"
         hx-trigger="load"
         hx-swap="innerHTML">
        <div class="loading">Laen...</div>
    </div>
`)
