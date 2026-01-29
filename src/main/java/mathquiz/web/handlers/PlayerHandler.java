package mathquiz.web.handlers;

import io.javalin.http.Context;
import mathquiz.domain.User;
import mathquiz.storage.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Handles player selection and creation.
 */
public class PlayerHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerHandler.class);
    
    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USER_NAME = "userName";
    public static final String COOKIE_USER_ID = "rememberedUserId";
    private static final int COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60; // 30 days
    
    private final Repository repo;
    
    public PlayerHandler(Repository repo) {
        this.repo = repo;
    }
    
    /**
     * GET /players - Show player list and create form.
     */
    public void listPlayers(Context ctx) {
        List<User> players = repo.getUsers();
        ctx.render("players.jte", Map.of("players", players));
    }
    
    /**
     * POST /players - Create a new player.
     */
    public void createPlayer(Context ctx) {
        String name = ctx.formParam("name");
        
        if (name == null || name.isBlank()) {
            ctx.redirect("/players");
            return;
        }
        
        name = name.trim();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        
        try {
            User user = repo.createUser(name);
            log.info("Created player: {} (id={})", user.name(), user.id());
        } catch (Exception e) {
            log.warn("Failed to create player '{}': {}", name, e.getMessage());
        }
        
        ctx.redirect("/players");
    }
    
    /**
     * POST /players/{id}/select - Select a player and start quiz.
     */
    public void selectPlayer(Context ctx) {
        int userId = ctx.pathParamAsClass("id", Integer.class).get();
        
        repo.getUser(userId).ifPresentOrElse(
            user -> {
                ctx.sessionAttribute(SESSION_USER_ID, user.id());
                ctx.sessionAttribute(SESSION_USER_NAME, user.name());
                ctx.cookie(COOKIE_USER_ID, String.valueOf(user.id()), COOKIE_MAX_AGE_SECONDS);
                log.info("Player selected: {} (id={})", user.name(), user.id());
                ctx.redirect("/quiz");
            },
            () -> {
                log.warn("Player not found: {}", userId);
                ctx.redirect("/players");
            }
        );
    }
    
    /**
     * Check if a user is logged in.
     */
    public static boolean isLoggedIn(Context ctx) {
        return ctx.sessionAttribute(SESSION_USER_ID) != null;
    }
    
    /**
     * Get the logged-in user ID.
     */
    public static Integer getUserId(Context ctx) {
        return ctx.sessionAttribute(SESSION_USER_ID);
    }
    
    /**
     * Get the logged-in user name.
     */
    public static String getUserName(Context ctx) {
        return ctx.sessionAttribute(SESSION_USER_NAME);
    }
    
    /**
     * Try to restore session from persistent cookie.
     * Called by before filter when session doesn't exist.
     */
    public void tryRestoreFromCookie(Context ctx) {
        if (isLoggedIn(ctx)) {
            return; // Already logged in
        }
        
        String cookieValue = ctx.cookie(COOKIE_USER_ID);
        if (cookieValue == null || cookieValue.isBlank()) {
            return;
        }
        
        try {
            int userId = Integer.parseInt(cookieValue);
            repo.getUser(userId).ifPresent(user -> {
                ctx.sessionAttribute(SESSION_USER_ID, user.id());
                ctx.sessionAttribute(SESSION_USER_NAME, user.name());
                log.info("Session restored from cookie for: {} (id={})", user.name(), user.id());
            });
        } catch (NumberFormatException e) {
            log.warn("Invalid rememberedUserId cookie value: {}", cookieValue);
        }
    }
}
