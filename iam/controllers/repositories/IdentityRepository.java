package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

import MachinaEar.iam.entities.Identity;

@ApplicationScoped
public class IdentityRepository {

    private final MongoCollection<Identity> col;

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

    public Identity create(Identity i) { col.insertOne(i); return i; }

    public void update(Identity i) { col.replaceOne(eq("_id", i.getId()), i); }

    public boolean emailExists(String email) { return col.countDocuments(eq("email", email)) > 0; }
}
