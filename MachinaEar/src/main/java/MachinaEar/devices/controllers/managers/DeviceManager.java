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

    // --- Pairing Logic ---

    public Device registerPendingDevice(String pairingCode, String mac, String hostname) {
        // Check if device with this MAC already exists and is pending
        // For simplicity, we create a new entry or update existing pending one
        Device device = devices.findByPairingCode(pairingCode).orElse(new Device());
        
        device.setName(hostname);
        device.setType("iot"); // Default type for Raspberry Pi devices
        device.setMac(mac);
        device.setPairingCode(pairingCode);
        device.setIsPaired(false);
        device.setStatus("pending_pairing");
        device.setExpiresAt(java.time.Instant.now().plus(java.time.Duration.ofMinutes(15)));
        
        if (device.getId() == null) {
            return devices.create(device);
        } else {
            devices.update(device);
            return device;
        }
    }

    public Device getDeviceByPairingCode(String pairingCode) {
        return devices.findByPairingCode(pairingCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired pairing code"));
    }

    public List<Device> getAvailableDevices() {
        return devices.findAvailableDevices();
    }

    public Device pairDevice(ObjectId userId, String pairingCode, String deviceName) {
        Device device = devices.findByPairingCode(pairingCode)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with provided code"));

        if (device.getIsPaired()) {
            throw new IllegalArgumentException("Device already paired");
        }

        // Ideally check if expired
        if (device.getExpiresAt() != null && device.getExpiresAt().isBefore(java.time.Instant.now())) {
            throw new IllegalArgumentException("Pairing code expired");
        }

        device.setIdentityId(userId);
        device.setName(deviceName);
        if (device.getType() == null) {
            device.setType("iot"); // Default type for IoT devices
        }
        device.setIsPaired(true);
        device.setStatus("normal");
        device.setPairingCode(null); // Clear code after pairing
        
        // Generate a simple device token (in production use JWT)
        device.setDeviceToken(java.util.UUID.randomUUID().toString());
        
        devices.update(device);
        return device;
    }
}

