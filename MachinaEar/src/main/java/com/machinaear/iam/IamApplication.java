package com.machinaear.iam;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.annotation.PreDestroy;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.codecs.configuration.CodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.*;
import org.bson.codecs.pojo.PojoCodecProvider;

import jakarta.annotation.PostConstruct;
import MachinaEar.iam.entities.Client;
import java.util.Arrays;

@ApplicationScoped
@ApplicationPath("/iam") // Base REST: http://host/.../iam/...
@OpenAPIDefinition(
    info = @Info(
        title = "MachinaEar IAM API",
        version = "0.1.0",
        description = "Identity and Access Management API with JWT authentication",
        contact = @Contact(
            name = "MachinaEar Team"
        ),
        license = @License(
            name = "Apache 2.0"
        )
    ),
    servers = {
        @Server(url = "https://localhost:8443/iam-0.1.0/iam", description = "Local Development Server (HTTPS)")
    }
)
public class IamApplication extends Application {

    private MongoClient client;

    @PostConstruct
    public void init() {
        seedDatabase();
    }

    private void seedDatabase() {
        try {
            MongoDatabase db = mongoDatabase(mongoClient());
            MongoCollection<Client> clientCol = db.getCollection("oauth_clients", Client.class);
            
            String clientId = "machina-ear-web";
            if (clientCol.countDocuments(com.mongodb.client.model.Filters.eq("clientId", clientId)) == 0) {
                Client webClient = new Client();
                webClient.setClientId(clientId);
                webClient.setClientName("MachinaEar Web Application");
                webClient.setClientType("public");
                webClient.setRedirectUris(Arrays.asList(
                    "http://localhost:3000/auth/callback",
                    "https://www.machinaear.me/auth/callback",
                    "https://machinaear.me/auth/callback"
                ));
                webClient.setAllowedScopes(Arrays.asList("openid", "email", "profile"));
                webClient.setActive(true);
                clientCol.insertOne(webClient);
                System.out.println("Seeded database with default client: " + clientId);
            }
        } catch (Exception e) {
            System.err.println("Database seeding failed: " + e.getMessage());
        }
    }

    @Produces @ApplicationScoped
    public MongoClient mongoClient() {
        if (client == null) {
            String uri = System.getProperty("MONGODB_URI",
                    System.getenv().getOrDefault("MONGODB_URI","mongodb://localhost:27017"));
            CodecRegistry pojoRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .codecRegistry(pojoRegistry)
                    .build();
            client = MongoClients.create(settings);
        }
        return client;
    }

    @Produces @ApplicationScoped
    public MongoDatabase mongoDatabase(MongoClient client) {
        String db = System.getProperty("MONGODB_DB",
                System.getenv().getOrDefault("MONGODB_DB", "machinaear"));
        return client.getDatabase(db);
    }

    @PreDestroy
    public void cleanup() { if (client != null) client.close(); }
}
