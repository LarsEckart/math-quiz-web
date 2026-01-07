package mathquiz.tts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Content-addressed TTS audio cache using Neurokõne API.
 * 
 * <p>Audio files are stored as {hash}.wav where hash = sha256(speaker|text).
 * This allows serving audio without database lookups.
 */
public class TtsCacheService {
    private static final Logger log = LoggerFactory.getLogger(TtsCacheService.class);
    
    private static final String API_URL = "https://api.tartunlp.ai/text-to-speech/v2";
    private static final String DEFAULT_SPEAKER = "liivika";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    
    private final Path cacheDir;
    private final String speaker;
    private final HttpClient httpClient;
    private final boolean enabled;
    
    /**
     * Create TTS cache service.
     * 
     * @param cacheDir directory to store cached .wav files
     * @param speaker Neurokõne speaker name (e.g., "liivika", "mari", "peeter")
     */
    public TtsCacheService(Path cacheDir, String speaker) {
        this.cacheDir = cacheDir;
        this.speaker = speaker != null ? speaker : DEFAULT_SPEAKER;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.enabled = true;
        
        // Ensure cache directory exists
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            log.error("Failed to create TTS cache directory: {}", cacheDir, e);
        }
    }
    
    /**
     * Create TTS cache service with default speaker.
     */
    public TtsCacheService(Path cacheDir) {
        this(cacheDir, DEFAULT_SPEAKER);
    }
    
    /**
     * Create a disabled TTS service (for testing).
     */
    public static TtsCacheService disabled() {
        return new TtsCacheService(null, null, false);
    }
    
    private TtsCacheService(Path cacheDir, String speaker, boolean enabled) {
        this.cacheDir = cacheDir;
        this.speaker = speaker;
        this.httpClient = null;
        this.enabled = enabled;
    }
    
    /**
     * Compute content-addressed hash for a text.
     * The hash includes the speaker so different voices get different cache entries.
     */
    public String computeHash(String text) {
        String key = speaker + "|" + text;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            // Use first 16 bytes (32 hex chars) for shorter filenames
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Get the cache file path for a hash.
     */
    public Path getCachePath(String hash) {
        return cacheDir.resolve(hash + ".wav");
    }
    
    /**
     * Check if audio is cached for the given text.
     */
    public boolean isCached(String text) {
        if (!enabled) return false;
        String hash = computeHash(text);
        return Files.exists(getCachePath(hash));
    }
    
    /**
     * Get audio hash for text, fetching from API if not cached.
     * 
     * @return hash if audio is available, empty if failed
     */
    public Optional<String> getAudioHash(String text) {
        if (!enabled) {
            return Optional.empty();
        }
        
        String hash = computeHash(text);
        Path cachePath = getCachePath(hash);
        
        // Check cache first
        if (Files.exists(cachePath)) {
            log.debug("TTS cache hit for hash: {}", hash);
            return Optional.of(hash);
        }
        
        // Fetch from API
        log.info("TTS cache miss, fetching: {} -> {}", text.substring(0, Math.min(30, text.length())), hash);
        
        Optional<byte[]> audioData = fetchFromApi(text);
        if (audioData.isEmpty()) {
            return Optional.empty();
        }
        
        // Write atomically to cache
        try {
            Path tempFile = Files.createTempFile(cacheDir, "tts-", ".wav.tmp");
            try {
                Files.write(tempFile, audioData.get());
                Files.move(tempFile, cachePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                log.debug("TTS cached: {}", hash);
                return Optional.of(hash);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.error("Failed to cache TTS audio: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Fetch audio from Neurokõne API.
     */
    private Optional<byte[]> fetchFromApi(String text) {
        String json = String.format("""
            {"text": "%s", "speaker": "%s"}
            """, escapeJson(text), speaker);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                return Optional.of(response.body());
            } else {
                log.warn("Neurokõne API returned status: {}", response.statusCode());
                return Optional.empty();
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Neurokõne API request failed: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
    
    /**
     * Simple JSON string escaping.
     */
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Stream a cached audio file.
     * 
     * @return input stream if file exists, empty otherwise
     */
    public Optional<InputStream> streamAudio(String hash) {
        if (!enabled) {
            return Optional.empty();
        }
        
        Path cachePath = getCachePath(hash);
        if (!Files.exists(cachePath)) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(Files.newInputStream(cachePath));
        } catch (IOException e) {
            log.warn("Failed to open cached audio: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
