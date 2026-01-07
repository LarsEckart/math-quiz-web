package mathquiz.web;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class Routes {
    
    public static void configure(Javalin app) {
        app.get("/health", Routes::health);
        app.get("/", Routes::index);
    }
    
    private static void health(Context ctx) {
        ctx.result("ok");
    }
    
    private static void index(Context ctx) {
        ctx.render("index.jte", Map.of("title", "Math Quiz"));
    }
}
