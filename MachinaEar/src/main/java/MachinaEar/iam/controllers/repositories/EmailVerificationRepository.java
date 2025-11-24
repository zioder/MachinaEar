package MachinaEar.iam.controllers.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.entities.EmailVerification;

import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;

/**
 * Repository for email verification operations
 */
@ApplicationScoped
public class EmailVerificationRepository {

    @Inject
    MongoDatabase db;

    private MongoCollection<EmailVerification> collection() {
        return db.getCollection("email_verifications", EmailVerification.class);
    }

    /**
     * Create a new email verification entry
     */
    public void create(EmailVerification verification) {
        collection().insertOne(verification);
    }

    /**
     * Find verification by token
     */
    public Optional<EmailVerification> findByToken(String token) {
        return Optional.ofNullable(
            collection().find(eq("token", token)).first()
        );
    }

    /**
     * Find verification by email
     */
    public Optional<EmailVerification> findByEmail(String email) {
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
     * Mark email as verified
     */
    public void markAsVerified(String token) {
        collection().updateOne(
            eq("token", token),
            set("verified", true)
        );
    }

    /**
     * Delete verification entry
     */
    public void delete(String token) {
        collection().deleteOne(eq("token", token));
    }

    /**
     * Cleanup expired verification tokens
     */
    public long deleteExpired() {
        return collection().deleteMany(
            lt("expiresAt", Instant.now())
        ).getDeletedCount();
    }
}
