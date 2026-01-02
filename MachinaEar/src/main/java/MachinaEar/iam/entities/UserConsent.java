package MachinaEar.iam.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Records user consent for OAuth client access
 * Stores what scopes a user has granted to which clients
 */
public class UserConsent extends RootEntity {
    private String identityId;        // User who granted consent (ObjectId hex)
    private String clientId;          // OAuth client
    private List<String> grantedScopes = new ArrayList<>();  // Scopes user approved
    private Instant grantedAt = Instant.now();
    private Instant expiresAt;        // Optional consent expiration
    private boolean revoked = false;  // User can revoke consent

    public UserConsent() {}

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public List<String> getGrantedScopes() { return grantedScopes; }
    public void setGrantedScopes(List<String> grantedScopes) { this.grantedScopes = grantedScopes; }

    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Check if consent covers all requested scopes
     */
    public boolean coversScopes(List<String> requestedScopes) {
        if (!isValid()) return false;
        if (requestedScopes == null || requestedScopes.isEmpty()) return true;
        return grantedScopes.containsAll(requestedScopes);
    }
}
