package MachinaEar.iam.entities;

import java.time.Instant;

public class Grant extends RootEntity {
    private GrantPK key;
    private Instant assignedAt = Instant.now();

    public GrantPK getKey() { return key; }
    public void setKey(GrantPK key) { this.key = key; }
    public Instant getAssignedAt() { return assignedAt; }
}
