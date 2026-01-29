package mathquiz.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import mathquiz.storage.Repository;
import mathquiz.tts.TtsCacheService;
import mathquiz.web.handlers.AudioHandler;
import mathquiz.web.handlers.PlayerHandler;
import mathquiz.web.handlers.QuizHandler;

import java.time.Clock;

/**
 * Configures all web routes.
 */
public class Routes {
    
    private final PlayerHandler playerHandler;
    private final QuizHandler quizHandler;
    private final AudioHandler audioHandler;
    
    public Routes(Repository repo, Clock clock, TtsCacheService ttsService) {
        this.playerHandler = new PlayerHandler(repo);
        this.quizHandler = new QuizHandler(repo, clock, ttsService);
        this.audioHandler = new AudioHandler(ttsService);
    }
    
    public void configure(Javalin app) {
        // Restore session from cookie if needed (before all requests)
        app.before(playerHandler::tryRestoreFromCookie);
        
        // Health check
        app.get("/health", this::health);
        
        // Root redirects to players
        app.get("/", ctx -> ctx.redirect("/players"));
        
        // Player routes
        app.get("/players", playerHandler::listPlayers);
        app.post("/players", playerHandler::createPlayer);
        app.post("/players/{id}/select", playerHandler::selectPlayer);
        
        // Quiz routes
        app.get("/quiz", quizHandler::showQuiz);
        app.get("/quiz/problem", quizHandler::getProblem);
        app.post("/quiz/answer", quizHandler::submitAnswer);
        
        // Audio routes
        app.get("/audio/{filename}", audioHandler::serveAudio);
    }
    
    private void health(Context ctx) {
        ctx.result("ok");
    }
}
