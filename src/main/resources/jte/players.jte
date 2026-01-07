@import mathquiz.domain.User
@import java.util.List

@param List<User> players

@template.layout(title = "Math Quiz - Players", content = @`
    <h1>🧮 Math Quiz</h1>

    <section class="player-section">
        <h2>Vali mängija</h2>

        @if(players.isEmpty())
            <p class="no-players">Mängijaid pole veel. Loo uus!</p>
        @else
            <div class="player-list">
                @for(User player : players)
                    <form action="/players/${player.id()}/select" method="post" class="player-form">
                        <button type="submit" class="player-button">
                            ${player.name()}
                        </button>
                    </form>
                @endfor
            </div>
        @endif
    </section>

    <section class="create-section">
        <h2>Loo uus mängija</h2>
        <form action="/players" method="post" class="create-form">
            <input type="text"
                   name="name"
                   placeholder="Nimi"
                   required
                   minlength="1"
                   maxlength="50"
                   autocomplete="off"
                   class="name-input">
            <button type="submit" class="create-button">Lisa</button>
        </form>
    </section>
`)
