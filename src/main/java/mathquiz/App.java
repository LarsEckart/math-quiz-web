package mathquiz;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import mathquiz.web.Routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        
        var app = createApp();
        app.start(port);
        
        log.info("Math Quiz started on port {}", port);
    }
    
    public static Javalin createApp() {
        var templateEngine = createTemplateEngine();
        
        var app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinJte(templateEngine));
        });
        
        Routes.configure(app);
        
        return app;
    }
    
    private static TemplateEngine createTemplateEngine() {
        // In development, use resource resolver for hot reload
        // In production, use precompiled templates
        var codeResolver = new ResourceCodeResolver("jte");
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }
}
