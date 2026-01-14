package MachinaEar.iam.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ALTCHA (Alternative CAPTCHA) Manager
 * 
 * Implements a proof-of-work challenge-response mechanism to protect
 * authentication endpoints from automated attacks without requiring
 * external services like reCAPTCHA.
 * 
 * The client must solve a computational puzzle (find a number that when
 * combined with the challenge produces a hash with the required difficulty).
 */
@ApplicationScoped
public class AltchaManager {

    // HMAC key for signing challenges - should be set via environment variable in production
    private static final String HMAC_SECRET = System.getenv("ALTCHA_HMAC_SECRET") != null 
        ? System.getenv("ALTCHA_HMAC_SECRET") 
        : "b01f014b87d5f02b2293d310bdb64faba10bdca0f6bfc70ae9facfb402e13b1c";
    
    // Challenge configuration
    private static final int MIN_COMPLEXITY = 50000;  // Minimum number range
    private static final int MAX_COMPLEXITY = 100000; // Maximum number range
    private static final int CHALLENGE_EXPIRY_SECONDS = 300; // 5 minutes
    private static final String ALGORITHM = "SHA-256";
    
    // Track used challenges to prevent replay attacks
    private final Map<String, Instant> usedChallenges = new ConcurrentHashMap<>();
    
    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    public void init() {
        // Clean up expired challenges periodically could be done here
        // For simplicity, cleanup happens during verification
    }

    /**
     * Generate a new ALTCHA challenge
     * 
     * @return JSON object containing the challenge parameters
     */
    public JsonObject generateChallenge() {
        // Generate random salt
        byte[] saltBytes = new byte[12];
        random.nextBytes(saltBytes);
        String salt = Base64.getUrlEncoder().withoutPadding().encodeToString(saltBytes);
        
        // Generate random secret number (the answer)
        int secretNumber = random.nextInt(MAX_COMPLEXITY - MIN_COMPLEXITY) + MIN_COMPLEXITY;
        
        // Create challenge string with timestamp for expiry
        long timestamp = Instant.now().getEpochSecond();
        String challengeData = salt + "?" + "expires=" + (timestamp + CHALLENGE_EXPIRY_SECONDS);
        
        // Compute the expected hash (what client needs to match)
        String expectedHash = computeHash(challengeData + secretNumber);
        
        // Sign the challenge for verification
        String signature = computeHmac(challengeData + expectedHash);
        
        return Json.createObjectBuilder()
            .add("algorithm", ALGORITHM)
            .add("challenge", expectedHash)
            .add("salt", challengeData)
            .add("signature", signature)
            .add("maxnumber", MAX_COMPLEXITY)
            .build();
    }

    /**
     * Verify an ALTCHA response from the client
     * 
     * @param payload Base64-encoded JSON payload from client
     * @return true if verification passes
     */
    public boolean verify(String payload) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        try {
            // Decode the base64 payload
            String decoded = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
            
            // Parse JSON
            JsonObject response;
            try (var reader = Json.createReader(new java.io.StringReader(decoded))) {
                response = reader.readObject();
            }

            String algorithm = response.getString("algorithm", "");
            String challenge = response.getString("challenge", "");
            int number = response.getInt("number", -1);
            String salt = response.getString("salt", "");
            String signature = response.getString("signature", "");

            // Validate algorithm
            if (!ALGORITHM.equals(algorithm)) {
                return false;
            }

            // Check for replay attack
            if (usedChallenges.containsKey(challenge)) {
                return false;
            }

            // Extract and check expiry
            if (salt.contains("expires=")) {
                String[] parts = salt.split("expires=");
                if (parts.length == 2) {
                    long expires = Long.parseLong(parts[1]);
                    if (Instant.now().getEpochSecond() > expires) {
                        return false; // Challenge expired
                    }
                }
            }

            // Verify the signature
            String expectedSignature = computeHmac(salt + challenge);
            if (!MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // Verify the solution
            String computedHash = computeHash(salt + number);
            if (!MessageDigest.isEqual(
                    challenge.getBytes(StandardCharsets.UTF_8),
                    computedHash.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // Mark challenge as used
            usedChallenges.put(challenge, Instant.now());
            
            // Cleanup old challenges
            cleanupExpiredChallenges();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compute SHA-256 hash
     */
    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    /**
     * Compute HMAC-SHA256
     */
    private String computeHmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Clean up expired challenges from the used challenges map
     */
    private void cleanupExpiredChallenges() {
        Instant cutoff = Instant.now().minusSeconds(CHALLENGE_EXPIRY_SECONDS * 2L);
        usedChallenges.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
