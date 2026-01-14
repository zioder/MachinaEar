package MachinaEar.devices.entities;

import java.time.Instant;

import org.bson.types.ObjectId;

import MachinaEar.iam.entities.RootEntity;
import MachinaEar.iam.json.ObjectIdAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

public class Device extends RootEntity {

    private String name;
    private String type; // e.g., "Mobile", "Desktop", "IoT"
    @JsonbTypeAdapter(ObjectIdAdapter.class)
    private ObjectId identityId; // Owner of the device
    private String status; // "normal", "abnormal", "offline"
    private Instant lastHeartbeat; // Last time device checked in
    private Double temperature; // Device temperature (for monitoring)
    private Double cpuUsage; // CPU usage percentage
    private Double memoryUsage; // Memory usage percentage
    private String lastError; // Last error message if any

    // MQTT and Pairing fields
    private String pairingCode; // Temporary pairing code for device registration
    private String deviceToken; // JWT token for device authentication
    private String mac; // Device MAC address
    private Double anomalyScore; // Latest anomaly detection score (MSE)
    private Instant lastAnomalyDetection; // Timestamp of last anomaly detection
    private Boolean isPaired; // Whether device has completed pairing
    private Boolean isOnline; // Whether device is currently online
    private Instant expiresAt; // Pairing code expiration time

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

    public ObjectId getIdentityId() {
        return identityId;
    }

    public void setIdentityId(ObjectId identityId) {
        this.identityId = identityId;
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

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public Instant getLastAnomalyDetection() {
        return lastAnomalyDetection;
    }

    public void setLastAnomalyDetection(Instant lastAnomalyDetection) {
        this.lastAnomalyDetection = lastAnomalyDetection;
    }

    public Boolean getIsPaired() {
        return isPaired;
    }

    public void setIsPaired(Boolean isPaired) {
        this.isPaired = isPaired;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
