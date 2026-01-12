package MachinaEar.devices.controllers.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import MachinaEar.devices.entities.Device;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DeviceRepository {

    private MongoCollection<Device> col;

    public DeviceRepository() {
    }

    @Inject
    public DeviceRepository(MongoDatabase db) {
        this.col = db.getCollection("devices", Device.class);
    }

    public List<Device> findByIdentityId(ObjectId identityId) {
        return col.find(eq("identityId", identityId)).into(new ArrayList<>());
    }

    public long countByIdentityId(ObjectId identityId) {
        return col.countDocuments(eq("identityId", identityId));
    }

    public Optional<Device> findById(ObjectId id) {
        return Optional.ofNullable(col.find(eq("_id", id)).first());
    }

    public Optional<Device> findById(String idHex) {
        try {
            return findById(new ObjectId(idHex));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Device create(Device device) {
        col.insertOne(device);
        return device;
    }

    public void update(Device device) {
        col.replaceOne(eq("_id", device.getId()), device);
    }

    public void delete(ObjectId id) {
        col.deleteOne(eq("_id", id));
    }

    public List<Device> findPendingPairing() {
        return col.find(eq("isPaired", false)).into(new ArrayList<>());
    }

    public Optional<Device> findByPairingCode(String pairingCode) {
        return Optional.ofNullable(col.find(eq("pairingCode", pairingCode)).first());
    }

    public Optional<Device> findByMac(String mac) {
        return Optional.ofNullable(col.find(eq("mac", mac)).first());
    }

    public Optional<Device> findByDeviceToken(String deviceToken) {
        return Optional.ofNullable(col.find(eq("deviceToken", deviceToken)).first());
    }
}
