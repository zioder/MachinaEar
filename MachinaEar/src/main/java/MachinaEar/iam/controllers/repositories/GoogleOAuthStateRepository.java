package MachinaEar.iam.controllers.repositories;

import java.time.Instant;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Updates.set;

import MachinaEar.iam.entities.GoogleOAuthState;

/**
 * Repository for managing Google OAuth state tokens during authentication flow.
 * State tokens are temporary and expire after 15 minutes.
 */
@ApplicationScoped
public class GoogleOAuthStateRepository {

    private MongoCollection<GoogleOAuthState> col;

    // No-args constructor for CDI proxy
    public GoogleOAuthStateRepository() {}

    @Inject
    public GoogleOAuthStateRepository(MongoDatabase db) {
        this.col = db.getCollection("google_oauth_states", GoogleOAuthState.class);
    }

    // Constructor for testing (manual injection of mock collection)
    public GoogleOAuthStateRepository(MongoCollection<GoogleOAuthState> col) {
        this.col = col;
    }

    /**
     * Create a new OAuth state record
     */
    public GoogleOAuthState create(GoogleOAuthState state) {
        col.insertOne(state);
        return state;
    }

    /**
     * Find OAuth state by state token
     */
    public Optional<GoogleOAuthState> findByStateToken(String stateToken) {
        GoogleOAuthState state = col.find(eq("stateToken", stateToken)).first();
        return Optional.ofNullable(state);
    }

    /**
     * Mark state token as used (prevent replay attacks)
     */
    public void markAsUsed(String stateToken) {
        col.updateOne(eq("stateToken", stateToken), set("used", true));
    }

    /**
     * Delete a state token (cleanup)
     */
    public void delete(String stateToken) {
        col.deleteOne(eq("stateToken", stateToken));
    }

    /**
     * Delete all expired state tokens (for cleanup job)
     */
    public long deleteExpired() {
        return col.deleteMany(
            lt("expiresAt", Instant.now())
        ).getDeletedCount();
    }
}
