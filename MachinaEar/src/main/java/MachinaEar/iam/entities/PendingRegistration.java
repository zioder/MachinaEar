package MachinaEar.iam.entities;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Stores pending user registrations awaiting email verification.
 * The actual Identity is NOT created until the verification code is confirmed.
 */
public class PendingRegistration extends RootEntity {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String email;
    private String username;
    private String passwordHash;      // Already hashed with Argon2
    private String verificationCode;  // 6-digit code
    private Instant expiresAt;        // Code expires after 15 minutes
    private int attempts;             // Track failed verification attempts
    private boolean verified;

    public PendingRegistration() {}

    public PendingRegistration(String email, String username, String passwordHash) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.verificationCode = generateCode();
        this.expiresAt = Instant.now().plusSeconds(15 * 60); // 15 minutes
        this.attempts = 0;
        this.verified = false;
    }

    /**
     * Generate a 6-digit verification code
     */
    private String generateCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(code);
    }

    /**
     * Regenerate the verification code (for resend)
     */
    public void regenerateCode() {
        this.verificationCode = generateCode();
        this.expiresAt = Instant.now().plusSeconds(15 * 60);
        this.attempts = 0;
    }

    /**
     * Check if the provided code matches
     */
    public boolean verifyCode(String code) {
        if (code == null || verificationCode == null) return false;
        return verificationCode.equals(code.trim());
    }

    /**
     * Increment failed attempts
     */
    public void incrementAttempts() {
        this.attempts++;
    }

    /**
     * Check if too many failed attempts (max 5)
     */
    public boolean isTooManyAttempts() {
        return attempts >= 5;
    }

    /**
     * Check if the code has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if this pending registration is still valid for verification
     */
    public boolean isValid() {
        return !verified && !isExpired() && !isTooManyAttempts();
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
}
