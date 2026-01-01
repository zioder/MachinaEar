package MachinaEar.iam.controllers.repositories;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import MachinaEar.iam.entities.RefreshToken;

@ApplicationScoped
public class RefreshTokenRepository {

    private MongoCollection<RefreshToken> col;

    // No-args constructor for CDI proxy
    public RefreshTokenRepository() {}

    @Inject
    public RefreshTokenRepository(MongoDatabase db) {
        this.col = db.getCollection("refresh_tokens", RefreshToken.class);
    }

    // Test constructor
    public RefreshTokenRepository(MongoCollection<RefreshToken> col) {
        this.col = col;
    }

    /**
     * Hashes a refresh token for secure storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Finds a refresh token by its hash
     */
    public Optional<RefreshToken> findByToken(String token) {
        String hash = hashToken(token);
        RefreshToken rt = col.find(eq("tokenHash", hash)).first();
        return Optional.ofNullable(rt);
    }

    /**
     * Creates a new refresh token record
     */
    public RefreshToken create(RefreshToken refreshToken) {
        col.insertOne(refreshToken);
        return refreshToken;
    }

    /**
     * Creates a new refresh token with hashing
     */
    public RefreshToken createWithToken(String token, String identityId, String clientId, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setTokenHash(hashToken(token));
        rt.setIdentityId(identityId);
        rt.setClientId(clientId);
        rt.setExpiresAt(expiresAt);
        rt.setRevoked(false);
        return create(rt);
    }

    /**
     * Updates a refresh token
     */
    public void update(RefreshToken refreshToken) {
        refreshToken.touch();
        col.replaceOne(eq("_id", refreshToken.getId()), refreshToken);
    }

    /**
     * Revokes a refresh token
     */
    public void revoke(String token) {
        String hash = hashToken(token);
        col.updateOne(
            eq("tokenHash", hash),
            combine(
                set("revoked", true),
                set("revokedAt", Instant.now())
            )
        );
    }

    /**
     * Revokes a refresh token and marks it as replaced
     */
    public void revokeAndReplace(String oldToken, String newTokenId) {
        String hash = hashToken(oldToken);
        col.updateOne(
            eq("tokenHash", hash),
            combine(
                set("revoked", true),
                set("revokedAt", Instant.now()),
                set("replacedByTokenId", newTokenId)
            )
        );
    }

    /**
     * Revokes all refresh tokens for a user
     */
    public void revokeAllForIdentity(String identityId) {
        col.updateMany(
            and(eq("identityId", identityId), eq("revoked", false)),
            combine(
                set("revoked", true),
                set("revokedAt", Instant.now())
            )
        );
    }

    /**
     * Cleans up expired and revoked tokens (maintenance task)
     */
    public long cleanupExpired() {
        var result = col.deleteMany(
            or(
                lt("expiresAt", Instant.now()),
                and(eq("revoked", true), lt("revokedAt", Instant.now().minusSeconds(30 * 24 * 60 * 60))) // 30 days
            )
        );
        return result.getDeletedCount();
    }
}
