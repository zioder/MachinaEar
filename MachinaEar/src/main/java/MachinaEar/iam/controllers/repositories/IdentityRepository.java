package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;

import MachinaEar.iam.entities.Identity;

@ApplicationScoped
public class IdentityRepository {

    private MongoCollection<Identity> col;

    // No-args constructor for CDI proxy
    public IdentityRepository() {}

    @Inject
    public IdentityRepository(MongoDatabase db) {
        this.col = db.getCollection("identities", Identity.class);
    }

    // Constructeur de test (injection manuelle d'une collection mock/IT)
    public IdentityRepository(MongoCollection<Identity> col) { this.col = col; }

    public Optional<Identity> findByEmail(String email) {
        Identity i = col.find(eq("email", email)).first();
        return Optional.ofNullable(i);
    }

    public Optional<Identity> findById(ObjectId id) {
        Identity i = col.find(eq("_id", id)).first();
        return Optional.ofNullable(i);
    }

    public Optional<Identity> findById(String idHex) {
        try {
            ObjectId id = new ObjectId(idHex);
            return findById(id);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Identity create(Identity i) {
        if (i.getId() == null) {
            i.setId(new ObjectId());
        }
        col.insertOne(i);
        return i;
    }

    public void update(Identity i) { col.replaceOne(eq("_id", i.getId()), i); }

    public boolean emailExists(String email) { return col.countDocuments(eq("email", email)) > 0; }

    /**
     * Find identity by OAuth provider and provider ID
     * Used for Google OAuth authentication
     */
    public Optional<Identity> findByOAuthProvider(String provider, String providerId) {
        Identity i = col.find(
            and(eq("oauthProvider", provider), eq("oauthProviderId", providerId))
        ).first();
        return Optional.ofNullable(i);
    }
}
