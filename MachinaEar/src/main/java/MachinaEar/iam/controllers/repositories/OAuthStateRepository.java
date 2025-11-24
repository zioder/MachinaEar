package MachinaEar.iam.controllers.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.entities.OAuthState;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

/**
 * Repository for OAuth state management
 * Stores OAuth parameters during authorization flow
 */
@ApplicationScoped
public class OAuthStateRepository {

    @Inject
    MongoDatabase db;

    private MongoCollection<OAuthState> collection() {
        return db.getCollection("oauth_states", OAuthState.class);
    }

    /**
     * Create a new OAuth state entry
     */
    public void create(OAuthState state) {
        collection().insertOne(state);
    }

    /**
     * Find OAuth state by state token
     */
    public Optional<OAuthState> findByStateToken(String stateToken) {
        return Optional.ofNullable(
            collection().find(eq("stateToken", stateToken)).first()
        );
    }

    /**
     * Mark OAuth state as used (single-use enforcement)
     */
    public void markAsUsed(String stateToken) {
        collection().updateOne(
            eq("stateToken", stateToken),
            set("used", true)
        );
    }

    /**
     * Delete OAuth state (cleanup after use)
     */
    public void delete(String stateToken) {
        collection().deleteOne(eq("stateToken", stateToken));
    }

    /**
     * Cleanup expired OAuth states
     * Should be called periodically (e.g., via scheduled task)
     */
    public long deleteExpired() {
        return collection().deleteMany(
            eq("used", true)
        ).getDeletedCount();
    }
}
