package MachinaEar.iam.entities;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bson.codecs.pojo.annotations.BsonIgnore;

import MachinaEar.iam.controllers.Role;

public class Identity extends RootEntity {
    private String tenantId;      // ObjectId hex string (optionnel au début)
    private String email;
    private String username;
    private String passwordHash;
    private boolean active = true;
    private Set<Role> roles = new LinkedHashSet<>();

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

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    @BsonIgnore
    public boolean hasRole(Role r) { return roles != null && roles.contains(r); }
}
