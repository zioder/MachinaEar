package MachinaEar.devices.controllers.managers;

import MachinaEar.devices.controllers.repositories.DeviceRepository;
import MachinaEar.devices.entities.Device;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

        if (status != null) {
            device.setStatus(status);
        }
        if (temperature != null) {
            device.setTemperature(temperature);
        }
        if (cpuUsage != null) {
            device.setCpuUsage(cpuUsage);
        }
        if (memoryUsage != null) {
            device.setMemoryUsage(memoryUsage);
        }
        if (lastError != null) {
            device.setLastError(lastError);
        }
        device.setLastHeartbeat(java.time.Instant.now());
        device.touch(); // Update timestamp

        devices.update(device);
        return device;
    }

    public Device registerPendingDevice(String pairingCode, String mac, String hostname) {
        // Check if device already exists with this MAC
        var existing = devices.findByMac(mac);
        if (existing.isPresent()) {
            Device device = existing.get();
            // Update pairing code and expiration
            device.setPairingCode(pairingCode);
            device.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
            devices.update(device);
            return device;
        }

        // Create new pending device
        Device device = new Device();
        device.setMac(mac);
        device.setName(hostname);
        device.setType("IoT");
        device.setPairingCode(pairingCode);
        device.setStatus("pending_pairing");
        device.setIsPaired(false);
        device.setIsOnline(false);
        device.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        device.setLastHeartbeat(Instant.now());

        return devices.create(device);
    }

    public List<Device> getAvailableDevices() {
        List<Device> pending = devices.findPendingPairing();
        // Filter out expired devices
        Instant now = Instant.now();
        return pending.stream()
                .filter(d -> d.getExpiresAt() != null && d.getExpiresAt().isAfter(now))
                .toList();
    }

    public Device pairDevice(ObjectId identityId, String pairingCode, String name) {
        Device device = devices.findByPairingCode(pairingCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pairing code"));

        // Check expiration
        if (device.getExpiresAt() != null && device.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Pairing code expired");
        }

        // Check device limit for this user
        if (devices.countByIdentityId(identityId) >= 5) {
            throw new IllegalArgumentException("Maximum number of devices (5) reached.");
        }

        // Generate device token (JWT)
        String deviceToken = generateDeviceToken(device.getId().toHexString(), device.getMac());

        // Update device
        device.setIdentityId(identityId);
        device.setName(name);
        device.setDeviceToken(deviceToken);
        device.setIsPaired(true);
        device.setIsOnline(true);  // Device is online when it gets paired
        device.setStatus("normal");
        device.setLastHeartbeat(Instant.now());  // Set initial heartbeat
        device.touch();

        devices.update(device);
        return device;
    }

    public Device getDeviceByPairingCode(String pairingCode) {
        return devices.findByPairingCode(pairingCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pairing code"));
    }

    public Device getDeviceByToken(String deviceToken) {
        return devices.findByDeviceToken(deviceToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid device token"));
    }

    public Device updateDeviceStatusByToken(String deviceToken, String status, Double anomalyScore) {
        Device device = devices.findByDeviceToken(deviceToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid device token"));

        if (status != null) {
            device.setStatus(status);
        }
        if (anomalyScore != null) {
            device.setAnomalyScore(anomalyScore);
        }
        device.setIsOnline(true);  // Mark device as online when it sends status
        device.setLastHeartbeat(Instant.now());
        device.touch();

        devices.update(device);
        return device;
    }

    private String generateDeviceToken(String deviceId, String mac) {
        try {
            // Use environment variable or default secret (in production, use proper secret management)
            String secret = System.getenv("DEVICE_TOKEN_SECRET");
            if (secret == null || secret.isEmpty()) {
                secret = "your-256-bit-secret-key-change-in-production-minimum-32-chars";
            }

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(deviceId)
                    .claim("mac", mac)
                    .claim("type", "device")
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)) // 1 year
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(secret.getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate device token", e);
        }
    }
}
