package MachinaEar.iam.controllers.repositories;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

import MachinaEar.iam.entities.Client;

@ApplicationScoped
public class ClientRepository {

    private MongoCollection<Client> col;

    // No-args constructor for CDI proxy
    public ClientRepository() {}

    @Inject
    public ClientRepository(MongoDatabase db) {
        this.col = db.getCollection("oauth_clients", Client.class);
    }

    // Test constructor
    public ClientRepository(MongoCollection<Client> col) { this.col = col; }

    public Optional<Client> findByClientId(String clientId) {
        Client c = col.find(eq("clientId", clientId)).first();
        return Optional.ofNullable(c);
    }

    public Client create(Client client) {
        col.insertOne(client);
        return client;
    }

    public void update(Client client) {
        col.replaceOne(eq("_id", client.getId()), client);
    }

    public boolean clientIdExists(String clientId) {
        return col.countDocuments(eq("clientId", clientId)) > 0;
    }
}
