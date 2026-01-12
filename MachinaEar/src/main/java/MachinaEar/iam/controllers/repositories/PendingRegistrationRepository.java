package MachinaEar.iam.controllers.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.entities.PendingRegistration;

import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

/**
 * Repository for pending registration operations
 */
@ApplicationScoped
public class PendingRegistrationRepository {

    @Inject
    MongoDatabase db;

    private MongoCollection<PendingRegistration> collection() {
        return db.getCollection("pending_registrations", PendingRegistration.class);
    }

    /**
     * Create a new pending registration entry
     */
    public void create(PendingRegistration pending) {
        // Remove any existing pending registration for this email
        collection().deleteMany(eq("email", pending.getEmail()));
        collection().insertOne(pending);
    }

    /**
     * Find pending registration by email
     */
    public Optional<PendingRegistration> findByEmail(String email) {
        return Optional.ofNullable(
            collection().find(
                and(
                    eq("email", email),
                    eq("verified", false)
                )
            ).first()
        );
    }

    /**
     * Update pending registration (for attempts, code regeneration, etc.)
     */
    public void update(PendingRegistration pending) {
        collection().replaceOne(
            eq("_id", pending.getId()),
            pending
        );
    }

    /**
     * Mark as verified and delete
     */
    public void markVerifiedAndDelete(String email) {
        collection().deleteMany(eq("email", email));
    }

    /**
     * Increment failed attempts for an email
     */
    public void incrementAttempts(String email) {
        collection().updateOne(
            eq("email", email),
            inc("attempts", 1)
        );
    }

    /**
     * Check if email has a pending registration
     */
    public boolean hasPending(String email) {
        return collection().countDocuments(
            and(
                eq("email", email),
                eq("verified", false),
                gt("expiresAt", Instant.now())
            )
        ) > 0;
    }

    /**
     * Cleanup expired pending registrations
     */
    public long deleteExpired() {
        return collection().deleteMany(
            lt("expiresAt", Instant.now())
        ).getDeletedCount();
    }
}
