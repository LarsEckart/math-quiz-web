package mathquiz.tts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TtsCacheServiceTest {

    @TempDir
    Path tempDir;

    private TtsCacheService service;

    @BeforeEach
    void setUp() {
        service = new TtsCacheService(tempDir, "liivika");
    }

    @Test
    void computeHash_deterministic() {
        String hash1 = service.computeHash("Kui palju on kaks pluss kolm?");
        String hash2 = service.computeHash("Kui palju on kaks pluss kolm?");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(32); // 16 bytes as hex
        assertThat(hash1).matches("[0-9a-f]+");
    }

    @Test
    void computeHash_differentForDifferentText() {
        String hash1 = service.computeHash("Kui palju on kaks pluss kolm?");
        String hash2 = service.computeHash("Kui palju on kolm pluss neli?");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void computeHash_differentForDifferentSpeaker() {
        TtsCacheService service1 = new TtsCacheService(tempDir, "liivika");
        TtsCacheService service2 = new TtsCacheService(tempDir, "mari");

        String hash1 = service1.computeHash("Hello");
        String hash2 = service2.computeHash("Hello");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void getCachePath_correctFormat() {
        String hash = service.computeHash("Test");
        Path path = service.getCachePath(hash);

        assertThat(path.getParent()).isEqualTo(tempDir);
        assertThat(path.getFileName().toString()).isEqualTo(hash + ".wav");
    }

    @Test
    void isCached_falseWhenNotCached() {
        assertThat(service.isCached("New text")).isFalse();
    }

    @Test
    void isCached_trueWhenCached() throws IOException {
        // Manually create a cached file
        String hash = service.computeHash("Cached text");
        Path cachePath = service.getCachePath(hash);
        Files.write(cachePath, new byte[]{1, 2, 3});

        assertThat(service.isCached("Cached text")).isTrue();
    }

    @Test
    void streamAudio_returnsCachedFile() throws IOException {
        // Manually create a cached file
        String hash = service.computeHash("Test audio");
        Path cachePath = service.getCachePath(hash);
        byte[] testData = {1, 2, 3, 4, 5};
        Files.write(cachePath, testData);

        Optional<InputStream> stream = service.streamAudio(hash);

        assertThat(stream).isPresent();
        assertThat(stream.get().readAllBytes()).isEqualTo(testData);
    }

    @Test
    void streamAudio_emptyWhenNotCached() {
        Optional<InputStream> stream = service.streamAudio("nonexistent");

        assertThat(stream).isEmpty();
    }

    @Test
    void disabled_alwaysReturnsFalseAndEmpty() {
        TtsCacheService disabled = TtsCacheService.disabled();

        assertThat(disabled.isCached("anything")).isFalse();
        assertThat(disabled.getAudioHash("anything")).isEmpty();
        assertThat(disabled.streamAudio("anything")).isEmpty();
    }

    // Note: We don't test actual API calls here - that would require mocking
    // or integration testing with the real Neurokõne API
}
