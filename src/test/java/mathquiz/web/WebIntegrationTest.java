package mathquiz.web;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import mathquiz.App;
import mathquiz.storage.DatabaseSetup;
import mathquiz.storage.Repository;
import mathquiz.storage.SqliteRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class WebIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private Javalin app;
    private Repository repo;
    private Clock clock;
    
    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test.db");
        var dbSetup = DatabaseSetup.setup(dbPath);
        repo = new SqliteRepository(dbSetup.jdbi());
        clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"));
        app = App.createApp(repo, clock);
    }
    
    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }
    
    @Test
    void healthEndpointReturnsOk() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/health");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("ok");
        });
    }
    
    @Test
    void rootRedirectsToPlayers() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            // OkHttp follows redirects by default
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Vali mängija");
        });
    }
    
    @Test
    void playersPageShowsEmptyList() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/players");
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Vali mängija");
            assertThat(body).contains("Mängijaid pole veel");
            assertThat(body).contains("Loo uus mängija");
        });
    }
    
    @Test
    void createPlayerAndShowInList() {
        JavalinTest.test(app, (server, client) -> {
            // Create a player
            var createResponse = client.post("/players", "name=TestPlayer");
            assertThat(createResponse.code()).isEqualTo(200); // Redirected and followed
            
            // Check player appears in list
            var listResponse = client.get("/players");
            String body = listResponse.body().string();
            assertThat(body).contains("TestPlayer");
            assertThat(body).doesNotContain("Mängijaid pole veel");
        });
    }
    
    @Test
    void selectPlayerSetsSessionAndRedirects() {
        JavalinTest.test(app, (server, client) -> {
            // Create player first
            repo.createUser("QuizPlayer");
            
            // Use a client with cookie jar to maintain session
            var cookieJar = new okhttp3.CookieJar() {
                private final java.util.List<okhttp3.Cookie> cookies = new java.util.ArrayList<>();
                
                @Override
                public void saveFromResponse(okhttp3.HttpUrl url, java.util.List<okhttp3.Cookie> cookies) {
                    this.cookies.addAll(cookies);
                }
                
                @Override
                public java.util.List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) {
                    return cookies;
                }
            };
            
            var sessionClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
            
            String baseUrl = "http://localhost:" + server.port();
            
            // Select the player
            var selectRequest = new Request.Builder()
                .url(baseUrl + "/players/1/select")
                .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                .build();
            var selectResponse = sessionClient.newCall(selectRequest).execute();
            assertThat(selectResponse.code()).isEqualTo(200); // Redirected to quiz
            
            String body = selectResponse.body().string();
            assertThat(body).contains("QuizPlayer");
            assertThat(body).contains("problem-area");
        });
    }
    
    @Test
    void quizPageWithoutSessionRedirectsToPlayers() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/quiz");
            assertThat(response.code()).isEqualTo(200);
            // Should redirect to players
            assertThat(response.body().string()).contains("Vali mängija");
        });
    }
    
    @Test
    void fullQuizFlow() {
        JavalinTest.test(app, (server, client) -> {
            // Create player
            repo.createUser("MathKid");
            
            // Use a client with cookie jar to maintain session
            var cookieJar = new okhttp3.CookieJar() {
                private final java.util.List<okhttp3.Cookie> cookies = new java.util.ArrayList<>();
                
                @Override
                public void saveFromResponse(okhttp3.HttpUrl url, java.util.List<okhttp3.Cookie> cookies) {
                    this.cookies.addAll(cookies);
                }
                
                @Override
                public java.util.List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) {
                    return cookies;
                }
            };
            
            var sessionClient = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
            
            String baseUrl = "http://localhost:" + server.port();
            
            // Select player
            var selectRequest = new Request.Builder()
                .url(baseUrl + "/players/1/select")
                .post(RequestBody.create("", MediaType.parse("application/x-www-form-urlencoded")))
                .build();
            sessionClient.newCall(selectRequest).execute();
            
            // Get problem
            var problemRequest = new Request.Builder()
                .url(baseUrl + "/quiz/problem")
                .get()
                .build();
            var problemResponse = sessionClient.newCall(problemRequest).execute();
            assertThat(problemResponse.code()).isEqualTo(200);
            String problemBody = problemResponse.body().string();
            
            // Should contain problem elements
            assertThat(problemBody).contains("class=\"problem");
            assertThat(problemBody).contains("class=\"operand\"");
            assertThat(problemBody).contains("class=\"operator\"");
            assertThat(problemBody).contains("answer-input");
            
            // Submit an answer
            var answerRequest = new Request.Builder()
                .url(baseUrl + "/quiz/answer")
                .post(RequestBody.create("answer=2", MediaType.parse("application/x-www-form-urlencoded")))
                .build();
            var answerResponse = sessionClient.newCall(answerRequest).execute();
            assertThat(answerResponse.code()).isEqualTo(200);
            String answerBody = answerResponse.body().string();
            
            // Should contain feedback (either correct or incorrect)
            assertThat(answerBody).contains("class=\"feedback");
            assertThat(answerBody).contains("feedback-icon");
            
            // Should have OOB stat updates
            assertThat(answerBody).contains("id=\"streak\"");
            assertThat(answerBody).contains("hx-swap-oob");
        });
    }
}
