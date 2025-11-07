package MachinaEar.iam.entities;

import java.io.Serializable;
import java.util.Objects;
import org.bson.types.ObjectId;
import MachinaEar.iam.controllers.Role;

public class GrantPK implements Serializable {
    private ObjectId identityId;
    private Role role;

    public GrantPK() {}
    public GrantPK(ObjectId identityId, Role role) {
        this.identityId = identityId; this.role = role;
    }

    public ObjectId getIdentityId() { return identityId; }
    public void setIdentityId(ObjectId identityId) { this.identityId = identityId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GrantPK)) return false;
        GrantPK that = (GrantPK) o;
        return Objects.equals(identityId, that.identityId) && role == that.role;
    }
    @Override public int hashCode() { return Objects.hash(identityId, role); }
}
