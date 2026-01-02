package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.and;

import MachinaEar.iam.entities.UserConsent;

@ApplicationScoped
public class UserConsentRepository {

    private MongoCollection<UserConsent> col;

    // No-args constructor for CDI proxy
    public UserConsentRepository() {}

    @Inject
    public UserConsentRepository(MongoDatabase db) {
        this.col = db.getCollection("user_consents", UserConsent.class);
    }

    // Test constructor
    public UserConsentRepository(MongoCollection<UserConsent> col) {
        this.col = col;
    }

    public Optional<UserConsent> findByIdentityAndClient(String identityId, String clientId) {
        UserConsent uc = col.find(and(
            eq("identityId", identityId),
            eq("clientId", clientId)
        )).first();
        return Optional.ofNullable(uc);
    }

    public UserConsent create(UserConsent consent) {
        col.insertOne(consent);
        return consent;
    }

    public void update(UserConsent consent) {
        consent.touch();
        col.replaceOne(eq("_id", consent.getId()), consent);
    }

    public void delete(UserConsent consent) {
        col.deleteOne(eq("_id", consent.getId()));
    }
}
