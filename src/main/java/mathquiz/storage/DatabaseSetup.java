package mathquiz.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;

/**
 * Database setup utilities: connection pooling, migrations, Jdbi configuration.
 */
public class DatabaseSetup {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSetup.class);

    /**
     * Create a HikariCP DataSource for SQLite.
     */
    public static HikariDataSource createDataSource(Path dbPath) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath.toAbsolutePath());
        config.setMaximumPoolSize(1); // SQLite only supports one writer
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    /**
     * Create a HikariCP DataSource for in-memory SQLite (for testing).
     */
    public static HikariDataSource createInMemoryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        // Keep connection open for in-memory DB
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
    }

    /**
     * Run Flyway migrations on the given data source.
     */
    public static void runMigrations(DataSource dataSource) {
        logger.info("Running database migrations...");
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();

        var result = flyway.migrate();
        logger.info("Migrations complete. Applied {} migrations.", result.migrationsExecuted);
    }

    /**
     * Create a configured Jdbi instance.
     */
    public static Jdbi createJdbi(DataSource dataSource) {
        return Jdbi.create(dataSource);
    }

    /**
     * Full setup: create data source, run migrations, return Jdbi.
     */
    public static SetupResult setup(Path dbPath) {
        HikariDataSource dataSource = createDataSource(dbPath);
        runMigrations(dataSource);
        Jdbi jdbi = createJdbi(dataSource);
        return new SetupResult(dataSource, jdbi);
    }

    /**
     * Full setup for in-memory database (for testing).
     */
    public static SetupResult setupInMemory() {
        HikariDataSource dataSource = createInMemoryDataSource();
        runMigrations(dataSource);
        Jdbi jdbi = createJdbi(dataSource);
        return new SetupResult(dataSource, jdbi);
    }

    /**
     * Result of database setup containing the data source and Jdbi instance.
     */
    public record SetupResult(HikariDataSource dataSource, Jdbi jdbi) implements AutoCloseable {
        @Override
        public void close() {
            dataSource.close();
        }
    }
}
