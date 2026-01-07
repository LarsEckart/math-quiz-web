package mathquiz.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DatabaseSetup utility.
 */
class DatabaseSetupTest {

    @Test
    void setup_withFilePath_createsDatabase(@TempDir Path tempDir) {
        Path dbPath = tempDir.resolve("test.db");

        try (var setup = DatabaseSetup.setup(dbPath)) {
            // Verify we can use the database
            var repo = new SqliteRepository(setup.jdbi());
            var user = repo.createUser("Test");

            assertThat(user.name()).isEqualTo("Test");
        }

        // Verify file was created
        assertThat(dbPath).exists();
    }

    @Test
    void setup_runsMigrations(@TempDir Path tempDir) {
        Path dbPath = tempDir.resolve("test.db");

        try (var setup = DatabaseSetup.setup(dbPath)) {
            // Verify tables exist by checking schema
            var tables = setup.jdbi().withHandle(handle ->
                handle.createQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
                ).mapTo(String.class).list()
            );

            assertThat(tables).contains(
                "attempts",
                "daily_stats",
                "flyway_schema_history",
                "operation_progress",
                "problem_stats",
                "users"
            );
        }
    }

    @Test
    void setupInMemory_worksCorrectly() {
        try (var setup = DatabaseSetup.setupInMemory()) {
            var repo = new SqliteRepository(setup.jdbi());
            var user = repo.createUser("Test");

            assertThat(repo.getUser(user.id())).isPresent();
        }
    }
}
