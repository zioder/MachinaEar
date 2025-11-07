package MachinaEar.iam.entities;

public class Tenant extends RootEntity {
    private String key;
    private String name;
    private boolean active = true;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
