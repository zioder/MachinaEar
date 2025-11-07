package MachinaEar.iam.controllers.repositories;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import MachinaEar.iam.entities.Grant;
import MachinaEar.iam.entities.GrantPK;
import MachinaEar.iam.controllers.Role;

import org.bson.types.ObjectId;

@ApplicationScoped
public class GrantRepository {

    private final MongoCollection<Grant> col;

    @Inject
    public GrantRepository(MongoDatabase db) {
        this.col = db.getCollection("grants", Grant.class);
    }

    public List<Role> findRolesByIdentity(ObjectId identityId) {
        List<Role> roles = new ArrayList<>();
        for (Grant g : col.find(eq("key.identityId", identityId))) {
            roles.add(g.getKey().getRole());
        }
        return roles;
    }

    public void grant(ObjectId identityId, Role role) {
        Grant g = new Grant();
        g.setKey(new GrantPK(identityId, role));
        col.insertOne(g);
    }
}
