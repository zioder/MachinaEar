package MachinaEar.iam.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth 2.0 Client entity
 * Stores client application registrations with their allowed redirect URIs
 */
public class Client extends RootEntity {
    private String clientId;        // Unique client identifier
    private String clientName;      // Human-readable name
    private String clientType;      // "public" or "confidential"
    private List<String> redirectUris = new ArrayList<>();  // Allowed redirect URIs
    private List<String> allowedScopes = new ArrayList<>(); // OAuth scopes this client can request
    private String audience;        // Resource server identifier for JWT aud claim
    private boolean active = true;  // Whether client is active

    // For confidential clients (not used for public clients like SPAs)
    private String clientSecret;    // Hashed client secret

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public List<String> getRedirectUris() { return redirectUris; }
    public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }

    public List<String> getAllowedScopes() { return allowedScopes; }
    public void setAllowedScopes(List<String> allowedScopes) { this.allowedScopes = allowedScopes; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    /**
     * Validates if a redirect URI is allowed for this client
     * Uses EXACT matching for security (no prefix matching)
     */
    public boolean isRedirectUriAllowed(String redirectUri) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            return false;
        }
        // EXACT match only - prevents open redirect vulnerabilities
        // (e.g., prevents https://example.com from matching https://example.com.attacker.com)
        return redirectUris.contains(redirectUri);
    }

    /**
     * Validates if requested scopes are allowed for this client
     */
    public boolean areScopesAllowed(List<String> requestedScopes) {
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return true; // No scopes requested - always allowed
        }
        if (allowedScopes == null || allowedScopes.isEmpty()) {
            return false; // Client has no allowed scopes
        }
        return allowedScopes.containsAll(requestedScopes);
    }
}
