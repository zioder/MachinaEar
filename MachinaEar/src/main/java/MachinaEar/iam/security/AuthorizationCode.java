package MachinaEar.iam.security;

import java.time.Instant;

public class AuthorizationCode {
    private String code;
    private String clientId;
    private String subject;
    private String redirectUri;
    private Instant expiresAt;

    public AuthorizationCode() {}
    public AuthorizationCode(String code, String clientId, String subject, String redirectUri, Instant exp) {
        this.code = code; this.clientId = clientId; this.subject = subject; this.redirectUri = redirectUri; this.expiresAt = exp;
    }
    public String getCode() { return code; }
    public String getClientId() { return clientId; }
    public String getSubject() { return subject; }
    public String getRedirectUri() { return redirectUri; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
}
