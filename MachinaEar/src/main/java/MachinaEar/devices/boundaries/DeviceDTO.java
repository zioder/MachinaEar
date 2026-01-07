package MachinaEar.devices.boundaries;

import java.time.Instant;

import MachinaEar.devices.entities.Device;

/**
 * Data Transfer Object for Device to ensure proper JSON serialization
 */
public class DeviceDTO {

    private String id;
    private String name;
    private String type;
    private String status;
    private Instant lastHeartbeat;
    private Double temperature;
    private Double cpuUsage;
    private Double memoryUsage;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Pairing fields
    private String pairingCode;
    private String deviceToken;
    private String mac;
    private Boolean isPaired;
    private Instant expiresAt;

    public DeviceDTO() {
    }

    public DeviceDTO(Device device) {
        this.id = device.getId() != null ? device.getId().toHexString() : null;
        this.name = device.getName();
        this.type = device.getType();
        this.status = device.getStatus();
        this.lastHeartbeat = device.getLastHeartbeat();
        this.temperature = device.getTemperature();
        this.cpuUsage = device.getCpuUsage();
        this.memoryUsage = device.getMemoryUsage();
        this.lastError = device.getLastError();
        this.createdAt = device.getCreatedAt();
        this.updatedAt = device.getUpdatedAt();
        
        // Pairing fields
        this.pairingCode = device.getPairingCode();
        this.deviceToken = device.getDeviceToken();
        this.mac = device.getMac();
        this.isPaired = device.getIsPaired();
        this.expiresAt = device.getExpiresAt();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Pairing getters/setters
    public String getPairingCode() {
        return pairingCode;
    }

    public void setPairingCode(String pairingCode) {
        this.pairingCode = pairingCode;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Boolean getIsPaired() {
        return isPaired;
    }

    public void setIsPaired(Boolean isPaired) {
        this.isPaired = isPaired;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

