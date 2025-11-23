package MachinaEar.iam.security;

import java.time.Instant;
import org.bson.types.ObjectId;

import MachinaEar.iam.entities.RootEntity;

/**
 * OAuth 2.0 Authorization Code with PKCE support
 * Short-lived code that can be exchanged for tokens
 */
public class AuthorizationCode extends RootEntity {
    private String code;                    // The authorization code itself
    private String clientId;                // Which client this code is for
    private String identityId;              // Which user (Identity ObjectId as hex string)
    private String redirectUri;             // Where to redirect after exchange
    private Instant expiresAt;              // Typically 10 minutes from creation
    private boolean used = false;           // Authorization codes are single-use

    // PKCE fields
    private String codeChallenge;           // SHA256 hash of code_verifier
    private String codeChallengeMethod;     // "S256" or "plain"

    // Optional
    private String scope;                   // Requested scopes
    private String state;                   // CSRF protection token

    public AuthorizationCode() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public String getCodeChallenge() { return codeChallenge; }
    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }

    public String getCodeChallengeMethod() { return codeChallengeMethod; }
    public void setCodeChallengeMethod(String codeChallengeMethod) { this.codeChallengeMethod = codeChallengeMethod; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
