package mathquiz;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    
    private final Javalin app = App.createApp();
    
    @Test
    void healthEndpointReturnsOk() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/health");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("ok");
        });
    }
    
    @Test
    void indexPageRendersTemplate() {
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).contains("Math Quiz");
        });
    }
}
