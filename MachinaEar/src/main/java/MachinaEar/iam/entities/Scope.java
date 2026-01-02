package MachinaEar.iam.entities;

/**
 * OAuth 2.0 Scope definition
 * Scopes define what access permissions are granted to OAuth clients
 */
public class Scope extends RootEntity {
    private String name;              // e.g., "devices:read", "devices:write", "profile"
    private String description;       // Human-readable description
    private boolean active = true;    // Whether scope is currently valid

    public Scope() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Validates if this scope is valid for use
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && active;
    }
}
