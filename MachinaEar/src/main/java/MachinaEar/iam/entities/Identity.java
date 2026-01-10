package MachinaEar.iam.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bson.codecs.pojo.annotations.BsonIgnore;

import MachinaEar.iam.controllers.Role;

public class Identity extends RootEntity {
    private String tenantId;      // ObjectId hex string (optionnel au début)
    private String email;
    private String username;
    private String passwordHash;  // Nullable for Google-only accounts
    private boolean active = true;
    private boolean emailVerified = false; // Email verification status
    private Set<Role> roles = new LinkedHashSet<>();

    // 2FA fields
    private boolean twoFactorEnabled = false;
    private String twoFactorSecret;  // Base32 encoded secret for TOTP
    private List<String> recoveryCodes = new ArrayList<>(); // Hashed recovery codes

    // OAuth provider fields (for Google OAuth, GitHub, etc.)
    private String oauthProvider;        // "google", "github", null for local auth
    private String oauthProviderId;      // Provider's unique user ID (Google: "sub" claim)
    private String oauthProviderEmail;   // Email from OAuth provider
    private Instant lastOAuthSync;       // Last time OAuth data was synced

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    @BsonIgnore
    public boolean hasRole(Role r) { return roles != null && roles.contains(r); }

    // 2FA getters and setters
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String twoFactorSecret) { this.twoFactorSecret = twoFactorSecret; }

    public List<String> getRecoveryCodes() { return recoveryCodes; }
    public void setRecoveryCodes(List<String> recoveryCodes) { this.recoveryCodes = recoveryCodes; }

    // OAuth getters and setters
    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthProviderId() { return oauthProviderId; }
    public void setOauthProviderId(String oauthProviderId) { this.oauthProviderId = oauthProviderId; }

    public String getOauthProviderEmail() { return oauthProviderEmail; }
    public void setOauthProviderEmail(String oauthProviderEmail) { this.oauthProviderEmail = oauthProviderEmail; }

    public Instant getLastOAuthSync() { return lastOAuthSync; }
    public void setLastOAuthSync(Instant lastOAuthSync) { this.lastOAuthSync = lastOAuthSync; }
}
