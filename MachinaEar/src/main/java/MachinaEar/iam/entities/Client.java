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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    /**
     * Validates if a redirect URI is allowed for this client
     */
    public boolean isRedirectUriAllowed(String redirectUri) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            return false;
        }
        return redirectUris.stream().anyMatch(allowed -> redirectUri.startsWith(allowed));
    }
}
