package mathquiz.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import mathquiz.storage.Repository;
import mathquiz.web.handlers.PlayerHandler;
import mathquiz.web.handlers.QuizHandler;

import java.time.Clock;

/**
 * Configures all web routes.
 */
public class Routes {
    
    private final PlayerHandler playerHandler;
    private final QuizHandler quizHandler;
    
    public Routes(Repository repo, Clock clock) {
        this.playerHandler = new PlayerHandler(repo);
        this.quizHandler = new QuizHandler(repo, clock);
    }
    
    public void configure(Javalin app) {
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
    }
    
    private void health(Context ctx) {
        ctx.result("ok");
    }
}
