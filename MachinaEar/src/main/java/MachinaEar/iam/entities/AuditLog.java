package MachinaEar.iam.entities;

import java.time.Instant;
import java.util.Map;

/**
 * Audit log entity for tracking security events
 * Records authentication attempts, token operations, and security violations
 */
public class AuditLog extends RootEntity {

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        REGISTER_SUCCESS,
        REGISTER_FAILURE,
        LOGOUT,
        TOKEN_REFRESH_SUCCESS,
        TOKEN_REFRESH_FAILURE,
        TOKEN_EXCHANGE_SUCCESS,
        TOKEN_EXCHANGE_FAILURE,
        TWO_FA_SETUP,
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        TWO_FA_FAILURE,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        RATE_LIMIT_EXCEEDED,
        INVALID_OAUTH_CLIENT,
        INVALID_REDIRECT_URI,
        PKCE_VALIDATION_FAILURE
    }

    private EventType eventType;
    private String userEmail;      // Email of the user (if known)
    private String identityId;     // ObjectId hex string of identity
    private String ipAddress;      // Client IP address
    private String userAgent;      // Browser/client user agent
    private boolean success;       // Whether the operation succeeded
    private String details;        // Additional details or error message
    private Map<String, String> metadata; // Additional contextual data
    private Instant timestamp = Instant.now();

    public AuditLog() {}

    public AuditLog(EventType eventType, String userEmail, String ipAddress, boolean success) {
        this.eventType = eventType;
        this.userEmail = userEmail;
        this.ipAddress = ipAddress;
        this.success = success;
    }

    // Getters and setters
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
