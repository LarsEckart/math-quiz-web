package mathquiz;

import io.javalin.testtools.JavalinTest;
import mathquiz.storage.DatabaseSetup;
import mathquiz.storage.SqliteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void healthEndpointReturnsOk() {
        Path dbPath = tempDir.resolve("test.db");
        var dbSetup = DatabaseSetup.setup(dbPath);
        var repo = new SqliteRepository(dbSetup.jdbi());
        var app = App.createApp(repo, Clock.systemDefaultZone());
        
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/health");
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body().string()).isEqualTo("ok");
        });
    }
}
