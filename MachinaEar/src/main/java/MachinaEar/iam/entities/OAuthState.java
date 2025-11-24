package MachinaEar.iam.entities;

import java.time.Instant;

/**
 * OAuth authorization state stored in database
 * Replaces session-based storage for better scalability and session timeout handling
 *
 * Stores OAuth parameters between /authorize redirect to login and return to /authorize
 */
public class OAuthState extends RootEntity {

    private String stateToken;           // Unique state identifier (random token)
    private String clientId;
    private String redirectUri;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String state;                // CSRF state parameter from client
    private String scope;
    private Instant expiresAt;           // State expires after 15 minutes
    private boolean used = false;        // Prevent reuse

    public OAuthState() {}

    public OAuthState(String stateToken, String clientId, String redirectUri,
                      String codeChallenge, String codeChallengeMethod,
                      String state, String scope) {
        this.stateToken = stateToken;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
        this.state = state;
        this.scope = scope;
        this.expiresAt = Instant.now().plusSeconds(15 * 60); // 15 minutes
    }

    // Getters and setters
    public String getStateToken() { return stateToken; }
    public void setStateToken(String stateToken) { this.stateToken = stateToken; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }

    public String getCodeChallengeMethod() { return codeChallengeMethod; }
    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

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
