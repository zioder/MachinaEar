package MachinaEar.iam.entities;

import java.time.Instant;
import org.bson.types.ObjectId;
import org.bson.codecs.pojo.annotations.*;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import MachinaEar.iam.json.ObjectIdAdapter;

public abstract class RootEntity {
    @JsonbTypeAdapter(ObjectIdAdapter.class)
    @BsonId @BsonProperty("_id")
    private ObjectId id;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = Instant.now(); }
}
