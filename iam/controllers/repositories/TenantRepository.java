package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import MachinaEar.iam.entities.Tenant;

@ApplicationScoped
public class TenantRepository {

    private final MongoCollection<Tenant> col;

    @Inject
    public TenantRepository(MongoDatabase db) {
        this.col = db.getCollection("tenants", Tenant.class);
    }

    public Optional<Tenant> findByKey(String key) {
        return Optional.ofNullable(col.find(eq("key", key)).first());
    }
}
