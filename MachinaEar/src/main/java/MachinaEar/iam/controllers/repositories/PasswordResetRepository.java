package MachinaEar.iam.controllers.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.entities.PasswordReset;

import java.time.Instant;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;

/**
 * Repository for password reset operations
 */
@ApplicationScoped
public class PasswordResetRepository {

    @Inject
    MongoDatabase db;

    private MongoCollection<PasswordReset> collection() {
        return db.getCollection("password_resets", PasswordReset.class);
    }

    public void create(PasswordReset reset) {
        collection().insertOne(reset);
    }

    public Optional<PasswordReset> findByToken(String token) {
        return Optional.ofNullable(
            collection().find(eq("token", token)).first()
        );
    }

    public void markAsUsed(String token) {
        collection().updateOne(
            eq("token", token),
            set("used", true)
        );
    }

    public long deleteExpired() {
        return collection().deleteMany(
            lt("expiresAt", Instant.now())
        ).getDeletedCount();
    }
}
