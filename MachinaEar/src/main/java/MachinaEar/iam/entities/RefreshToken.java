package MachinaEar.iam.entities;

import java.time.Instant;

/**
 * OAuth 2.1 Refresh Token Entity
 * Tracks refresh tokens for rotation and revocation
 */
public class RefreshToken extends RootEntity {
    private String tokenHash;           // SHA-256 hash of the refresh token
    private String identityId;          // User who owns this token
    private String clientId;            // OAuth client that issued this token
    private Instant expiresAt;          // Expiration time
    private boolean revoked = false;    // Token revocation status
    private Instant revokedAt;          // When token was revoked
    private String replacedByTokenId;   // ID of the new token that replaced this one (for rotation)

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByTokenId() {
        return replacedByTokenId;
    }

    public void setReplacedByTokenId(String replacedByTokenId) {
        this.replacedByTokenId = replacedByTokenId;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
