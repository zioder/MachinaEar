package MachinaEar.iam.entities;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password reset entity
 * Stores reset tokens for password recovery
 */
public class PasswordReset extends RootEntity {

    private String email;
    private String token;           // Secure random token (base64url)
    private String identityId;      // ObjectId hex string
    private Instant expiresAt;      // Token expires after 1 hour
    private boolean used = false;

    public PasswordReset() {}

    public PasswordReset(String email, String identityId) {
        this.email = email;
        this.identityId = identityId;
        this.token = generateToken();
        this.expiresAt = Instant.now().plusSeconds(60 * 60); // 1 hour
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
