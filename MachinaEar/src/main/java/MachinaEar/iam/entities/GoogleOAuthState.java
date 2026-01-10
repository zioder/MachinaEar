package MachinaEar.iam.entities;

import java.time.Instant;

/**
 * Stores temporary OAuth state during Google's redirect flow.
 * When user is redirected to Google for authentication, we lose session/URL state.
 * This entity preserves all original OAuth parameters so we can resume the
 * authorization code flow after Google callback.
 */
public class GoogleOAuthState extends RootEntity {

    private String stateToken;              // Random CSRF token for security
    private String returnTo;                // Where to redirect after success

    // Original OAuth 2.1 authorization parameters (if initiated from OAuth flow)
    private String originalClientId;         // Client requesting authorization
    private String originalRedirectUri;      // Client's redirect URI
    private String originalCodeChallenge;    // PKCE code challenge
    private String originalCodeChallengeMethod; // PKCE method (S256)
    private String originalState;            // Original client's state token
    private String originalScope;            // Requested scopes

    private Instant expiresAt;              // State expires after 15 minutes
    private boolean used = false;            // Prevent reuse attacks

    // Constructor
    public GoogleOAuthState() {
        // Default expiration: 15 minutes from now
        this.expiresAt = Instant.now().plusSeconds(900);
    }

    // Getters and setters
    public String getStateToken() { return stateToken; }
    public void setStateToken(String stateToken) { this.stateToken = stateToken; }

    public String getReturnTo() { return returnTo; }
    public void setReturnTo(String returnTo) { this.returnTo = returnTo; }

    public String getOriginalClientId() { return originalClientId; }
    public void setOriginalClientId(String originalClientId) { this.originalClientId = originalClientId; }

    public String getOriginalRedirectUri() { return originalRedirectUri; }
    public void setOriginalRedirectUri(String originalRedirectUri) { this.originalRedirectUri = originalRedirectUri; }

    public String getOriginalCodeChallenge() { return originalCodeChallenge; }
    public void setOriginalCodeChallenge(String originalCodeChallenge) { this.originalCodeChallenge = originalCodeChallenge; }

    public String getOriginalCodeChallengeMethod() { return originalCodeChallengeMethod; }
    public void setOriginalCodeChallengeMethod(String originalCodeChallengeMethod) {
        this.originalCodeChallengeMethod = originalCodeChallengeMethod;
    }

    public String getOriginalState() { return originalState; }
    public void setOriginalState(String originalState) { this.originalState = originalState; }

    public String getOriginalScope() { return originalScope; }
    public void setOriginalScope(String originalScope) { this.originalScope = originalScope; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    // Helper methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean hasOriginalOAuthParams() {
        return originalClientId != null && !originalClientId.isBlank();
    }
}
