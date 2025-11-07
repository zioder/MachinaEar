package com.machinaear.iam;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.annotation.PreDestroy;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.codecs.configuration.CodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.*;
import org.bson.codecs.pojo.PojoCodecProvider;

@ApplicationScoped
@ApplicationPath("/iam") // Base REST: http://host/.../iam/...
public class IamApplication extends Application {

    private MongoClient client;

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
