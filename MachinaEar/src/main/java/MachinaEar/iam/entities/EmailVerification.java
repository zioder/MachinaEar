package MachinaEar.iam.entities;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Email verification entity
 * Stores verification tokens for email confirmation
 */
public class EmailVerification extends RootEntity {

    private String email;
    private String token;           // Secure random token (base64url)
    private String identityId;      // ObjectId hex string
    private Instant expiresAt;      // Token expires after 24 hours
    private boolean verified = false;

    public EmailVerification() {}

    public EmailVerification(String email, String identityId) {
        this.email = email;
        this.identityId = identityId;
        this.token = generateToken();
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60); // 24 hours
    }

    /**
     * Generate a secure random verification token
     */
    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32]; // 256 bits
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !verified && !isExpired();
    }
}
