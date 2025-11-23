package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;

import MachinaEar.iam.security.AuthorizationCode;

@ApplicationScoped
public class AuthorizationCodeRepository {

    private MongoCollection<AuthorizationCode> col;

    // No-args constructor for CDI proxy
    public AuthorizationCodeRepository() {}

    @Inject
    public AuthorizationCodeRepository(MongoDatabase db) {
        this.col = db.getCollection("authorization_codes", AuthorizationCode.class);
    }

    // Test constructor
    public AuthorizationCodeRepository(MongoCollection<AuthorizationCode> col) {
        this.col = col;
    }

    public Optional<AuthorizationCode> findByCode(String code) {
        AuthorizationCode ac = col.find(eq("code", code)).first();
        return Optional.ofNullable(ac);
    }

    public AuthorizationCode create(AuthorizationCode authCode) {
        col.insertOne(authCode);
        return authCode;
    }

    public void update(AuthorizationCode authCode) {
        authCode.touch();
        col.replaceOne(eq("_id", authCode.getId()), authCode);
    }

    /**
     * Mark an authorization code as used (for single-use enforcement)
     */
    public void markAsUsed(String code) {
        Optional<AuthorizationCode> opt = findByCode(code);
        if (opt.isPresent()) {
            AuthorizationCode ac = opt.get();
            ac.setUsed(true);
            update(ac);
        }
    }
}
