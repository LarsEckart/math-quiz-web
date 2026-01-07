package mathquiz.web.handlers;

import io.javalin.http.Context;
import mathquiz.tts.TtsCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

/**
 * Handles audio file serving from TTS cache.
 */
public class AudioHandler {
    private static final Logger log = LoggerFactory.getLogger(AudioHandler.class);
    
    // Cache for 1 year (audio content is immutable for a given hash)
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";
    
    private final TtsCacheService ttsService;
    
    public AudioHandler(TtsCacheService ttsService) {
        this.ttsService = ttsService;
    }
    
    /**
     * GET /audio/{hash}.wav - Serve cached audio file.
     */
    public void serveAudio(Context ctx) {
        String filename = ctx.pathParam("filename");
        
        // Extract hash from filename (remove .wav extension)
        if (!filename.endsWith(".wav")) {
            ctx.status(400).result("Invalid audio format");
            return;
        }
        
        String hash = filename.substring(0, filename.length() - 4);
        
        // Validate hash format (should be hex string)
        if (!hash.matches("[0-9a-f]{32}")) {
            ctx.status(400).result("Invalid audio hash");
            return;
        }
        
        Optional<InputStream> audioStream = ttsService.streamAudio(hash);
        
        if (audioStream.isEmpty()) {
            log.warn("Audio not found for hash: {}", hash);
            ctx.status(404).result("Audio not found");
            return;
        }
        
        ctx.contentType("audio/wav");
        ctx.header("Cache-Control", CACHE_CONTROL);
        ctx.result(audioStream.get());
    }
}
