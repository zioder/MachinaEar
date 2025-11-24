package MachinaEar.devices.controllers.managers;

import MachinaEar.devices.controllers.repositories.DeviceRepository;
import MachinaEar.devices.entities.Device;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class DeviceManager {

    @Inject
    DeviceRepository devices;

    public List<Device> getDevices(ObjectId identityId) {
        return devices.findByIdentityId(identityId);
    }

    public Device addDevice(ObjectId identityId, String name, String type) {
        if (devices.countByIdentityId(identityId) >= 5) {
            throw new IllegalArgumentException("Maximum number of devices (5) reached.");
        }

        Device device = new Device();
        device.setIdentityId(identityId);
        device.setName(name);
        device.setType(type);
        device.setStatus("normal"); // Initialize with normal status
        device.setLastHeartbeat(java.time.Instant.now());
        device.setCpuUsage(0.0);
        device.setMemoryUsage(0.0);
        device.setTemperature(0.0);
        
        return devices.create(device);
    }

    public Device updateDevice(ObjectId identityId, String deviceId, String name, String type) {
        Device device = devices.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (!device.getIdentityId().equals(identityId)) {
            throw new SecurityException("Unauthorized access to device");
        }

        device.setName(name);
        device.setType(type);
        device.touch(); // Update timestamp

        devices.update(device);
        return device;
    }

    public void deleteDevice(ObjectId identityId, String deviceId) {
        Device device = devices.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (!device.getIdentityId().equals(identityId)) {
            throw new SecurityException("Unauthorized access to device");
        }

        devices.delete(device.getId());
    }

    public Device updateDeviceStatus(ObjectId identityId, String deviceId, String status, 
                                     Double temperature, Double cpuUsage, Double memoryUsage, String lastError) {
        Device device = devices.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        if (!device.getIdentityId().equals(identityId)) {
            throw new SecurityException("Unauthorized access to device");
        }

        if (status != null) device.setStatus(status);
        if (temperature != null) device.setTemperature(temperature);
        if (cpuUsage != null) device.setCpuUsage(cpuUsage);
        if (memoryUsage != null) device.setMemoryUsage(memoryUsage);
        if (lastError != null) device.setLastError(lastError);
        device.setLastHeartbeat(java.time.Instant.now());
        device.touch(); // Update timestamp

        devices.update(device);
        return device;
    }
}
