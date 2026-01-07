package mathquiz;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import mathquiz.storage.DatabaseSetup;
import mathquiz.storage.Repository;
import mathquiz.storage.SqliteRepository;
import mathquiz.web.Routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String dataDir = System.getenv().getOrDefault("DATA_DIR", "data");
        
        // Initialize database
        java.nio.file.Path dbPath = java.nio.file.Path.of(dataDir, "quiz.db");
        // Ensure data directory exists
        try {
            java.nio.file.Files.createDirectories(dbPath.getParent());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create data directory", e);
        }
        var dbSetup = DatabaseSetup.setup(dbPath);
        Repository repo = new SqliteRepository(dbSetup.jdbi());
        
        var app = createApp(repo, Clock.systemDefaultZone());
        app.start(port);
        
        log.info("Math Quiz started on port {}", port);
    }
    
    public static Javalin createApp(Repository repo, Clock clock) {
        var templateEngine = createTemplateEngine();
        
        var app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinJte(templateEngine));
        });
        
        new Routes(repo, clock).configure(app);
        
        return app;
    }
    
    /**
     * Create app for testing (without database).
     */
    public static Javalin createApp() {
        return createApp(null, Clock.systemDefaultZone());
    }
    
    private static TemplateEngine createTemplateEngine() {
        var codeResolver = new ResourceCodeResolver("jte");
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }
}
