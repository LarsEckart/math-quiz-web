package mathquiz.web.handlers;

import io.javalin.http.Context;
import mathquiz.domain.Problem;
import mathquiz.service.AnswerResult;
import mathquiz.service.QuizService;
import mathquiz.storage.Repository;
import mathquiz.tts.EstonianSpeechFormatter;
import mathquiz.tts.TtsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles the quiz flow - problems and answers.
 */
public class QuizHandler {
    private static final Logger log = LoggerFactory.getLogger(QuizHandler.class);
    
    private static final String SESSION_QUIZ_SERVICE = "quizService";
    
    private final Repository repo;
    private final Clock clock;
    private final TtsCacheService ttsService;
    
    public QuizHandler(Repository repo, Clock clock, TtsCacheService ttsService) {
        this.repo = repo;
        this.clock = clock;
        this.ttsService = ttsService;
    }
    
    /**
     * GET /quiz - Show quiz shell page.
     */
    public void showQuiz(Context ctx) {
        if (!PlayerHandler.isLoggedIn(ctx)) {
            ctx.redirect("/players");
            return;
        }
        
        QuizService service = getOrCreateService(ctx);
        
        Map<String, Object> model = new HashMap<>();
        model.put("playerName", PlayerHandler.getUserName(ctx));
        model.put("streak", service.sessionStats().currentStreak());
        model.put("todayStars", service.dailyStats().starsEarned());
        model.put("totalStars", service.getTotalStars());
        
        ctx.render("quiz.jte", model);
    }
    
    /**
     * GET /quiz/problem - Get the next problem (HTMX fragment).
     */
    public void getProblem(Context ctx) {
        if (!PlayerHandler.isLoggedIn(ctx)) {
            ctx.redirect("/players");
            return;
        }
        
        QuizService service = getOrCreateService(ctx);
        Problem problem = service.getNextProblem(null);
        
        log.debug("Generated problem: {} for user {}", problem, PlayerHandler.getUserId(ctx));
        
        // Generate audio for the problem
        String speechText = EstonianSpeechFormatter.formatProblem(
                problem.operand1(), problem.operation(), problem.operand2());
        Optional<String> audioHash = ttsService.getAudioHash(speechText);
        
        Map<String, Object> model = new HashMap<>();
        model.put("operand1", problem.operand1());
        model.put("operand2", problem.operand2());
        model.put("operation", problem.operation());
        model.put("audioHash", audioHash.orElse(null));
        
        ctx.render("fragments/problem.jte", model);
    }
    
    /**
     * POST /quiz/answer - Submit an answer (HTMX fragment).
     */
    public void submitAnswer(Context ctx) {
        if (!PlayerHandler.isLoggedIn(ctx)) {
            ctx.redirect("/players");
            return;
        }
        
        QuizService service = getOrCreateService(ctx);
        
        // Parse answer
        String answerStr = ctx.formParam("answer");
        if (answerStr == null || answerStr.isBlank()) {
            // Re-show current problem
            ctx.redirect("/quiz/problem");
            return;
        }
        
        int answer;
        try {
            answer = Integer.parseInt(answerStr.trim());
        } catch (NumberFormatException e) {
            // Re-show current problem
            Problem current = service.currentProblem();
            if (current != null) {
                Map<String, Object> model = new HashMap<>();
                model.put("operand1", current.operand1());
                model.put("operand2", current.operand2());
                model.put("operation", current.operation());
                ctx.render("fragments/problem.jte", model);
            } else {
                ctx.redirect("/quiz/problem");
            }
            return;
        }
        
        // Capture the problem before submitting (it gets cleared)
        Problem problem = service.currentProblem();
        
        // Submit and get result
        AnswerResult result = service.submitAnswer(answer);
        
        log.info("Answer submitted: {} -> {} for user {}", 
                answerStr, result.correct() ? "correct" : "incorrect", 
                PlayerHandler.getUserId(ctx));
        
        // Generate feedback audio
        String feedbackText;
        if (result.correct()) {
            feedbackText = EstonianSpeechFormatter.formatCorrect(
                    problem.operand1(), problem.operation(), problem.operand2(), result.correctAnswer());
        } else {
            feedbackText = EstonianSpeechFormatter.formatIncorrect(
                    problem.operand1(), problem.operation(), problem.operand2(), result.correctAnswer());
        }
        Optional<String> audioHash = ttsService.getAudioHash(feedbackText);
        
        // Render feedback
        Map<String, Object> model = new HashMap<>();
        model.put("correct", result.correct());
        model.put("correctAnswer", result.correctAnswer());
        model.put("streak", result.streak());
        model.put("todayStars", service.dailyStats().starsEarned());
        model.put("totalStars", service.getTotalStars());
        model.put("rangeExpanded", result.rangeExpanded());
        model.put("newOperationUnlocked", result.newOperationUnlocked());
        model.put("newStars", result.newStars());
        model.put("audioHash", audioHash.orElse(null));
        
        ctx.render("fragments/feedback.jte", model);
    }
    
    /**
     * Get or create QuizService for the current session.
     */
    private QuizService getOrCreateService(Context ctx) {
        QuizService service = ctx.sessionAttribute(SESSION_QUIZ_SERVICE);
        Integer currentUserId = PlayerHandler.getUserId(ctx);
        
        // If service exists and is for the same user, return it
        // Otherwise create a new one
        if (service != null) {
            // Check if it's the same user (QuizService is tied to userId)
            // We can't easily check, so we'll recreate on each session for safety
            // Actually, let's store the userId with the service
            Integer serviceUserId = ctx.sessionAttribute("quizServiceUserId");
            if (currentUserId.equals(serviceUserId)) {
                return service;
            }
        }
        
        // Create new service
        service = new QuizService(repo, currentUserId, clock);
        ctx.sessionAttribute(SESSION_QUIZ_SERVICE, service);
        ctx.sessionAttribute("quizServiceUserId", currentUserId);
        
        return service;
    }
}
