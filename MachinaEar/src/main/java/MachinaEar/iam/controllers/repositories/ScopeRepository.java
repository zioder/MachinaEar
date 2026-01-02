package MachinaEar.iam.controllers.repositories;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

import MachinaEar.iam.entities.Scope;

@ApplicationScoped
public class ScopeRepository {

    private MongoCollection<Scope> col;

    // No-args constructor for CDI proxy
    public ScopeRepository() {}

    @Inject
    public ScopeRepository(MongoDatabase db) {
        this.col = db.getCollection("scopes", Scope.class);
    }

    // Test constructor
    public ScopeRepository(MongoCollection<Scope> col) {
        this.col = col;
    }

    public Optional<Scope> findByName(String name) {
        Scope s = col.find(eq("name", name)).first();
        return Optional.ofNullable(s);
    }

    public List<Scope> findByNames(List<String> names) {
        return col.find(in("name", names))
                  .into(new java.util.ArrayList<>());
    }

    public List<Scope> findAll() {
        return col.find().into(new java.util.ArrayList<>());
    }

    public Scope create(Scope scope) {
        col.insertOne(scope);
        return scope;
    }

    public void update(Scope scope) {
        scope.touch();
        col.replaceOne(eq("_id", scope.getId()), scope);
    }

    public boolean scopeExists(String name) {
        return col.countDocuments(eq("name", name)) > 0;
    }
}
