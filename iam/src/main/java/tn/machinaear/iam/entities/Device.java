package tn.machinaear.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Device Entity - Represents an IoT monitoring device in the MachinaEar system
 * Devices are Raspberry Pi units with vibration/acoustic sensors attached to machinery
 */
@Entity("devices")
public class Device implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("device_name")
    private String deviceName;

    @Column("machine_type")
    private String machineType;

    @Column("location")
    private String location;

    @Column("status")
    private String status;  // ACTIVE, INACTIVE, MAINTENANCE

    @Column("owner_id")
    private String ownerId;  // References Identity._id

    @Column("registration_date")
    private String registrationDate;

    @Column("last_seen")
    private String lastSeen;

    @Column("firmware_version")
    private String firmwareVersion;

    @Column("hardware_serial")
    private String hardwareSerial;

    @Column("description")
    private String description;

    // Predictive Maintenance Fields
    @Column("health_status")
    private String healthStatus;  // NORMAL, WARNING, POTENTIALLY_FAULTY, CRITICAL, FAULTY

    @Column("anomaly_score")
    private Double anomalyScore;  // 0.0 to 1.0 - ML model confidence score

    @Column("predicted_failure_time")
    private String predictedFailureTime;  // ISO timestamp when failure is predicted

    @Column("time_to_failure_minutes")
    private Integer timeToFailureMinutes;  // Minutes until predicted failure

    @Column("last_prediction_time")
    private String lastPredictionTime;  // When the last ML prediction was made

    @Column("alert_level")
    private String alertLevel;  // NONE, LOW, MEDIUM, HIGH, CRITICAL

    @Column("vibration_level")
    private Double vibrationLevel;  // Current vibration reading

    @Column("sound_level")
    private Double soundLevel;  // Current sound/noise level (dB)

    @Column("temperature")
    private Double temperature;  // Optional temperature reading

    @Column("prediction_message")
    private String predictionMessage;  // Human-readable prediction message

    // Constructors
    public Device() {
        this.id = UUID.randomUUID().toString();
        this.registrationDate = LocalDateTime.now().toString();
        this.status = "INACTIVE";
        this.healthStatus = "NORMAL";
        this.alertLevel = "NONE";
        this.anomalyScore = 0.0;
    }

    public Device(String deviceName, String machineType, String location, String ownerId) {
        this();
        this.deviceName = deviceName;
        this.machineType = machineType;
        this.location = location;
        this.ownerId = ownerId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareSerial() {
        return hardwareSerial;
    }

    public void setHardwareSerial(String hardwareSerial) {
        this.hardwareSerial = hardwareSerial;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Predictive Maintenance Getters and Setters
    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public String getPredictedFailureTime() {
        return predictedFailureTime;
    }

    public void setPredictedFailureTime(String predictedFailureTime) {
        this.predictedFailureTime = predictedFailureTime;
    }

    public Integer getTimeToFailureMinutes() {
        return timeToFailureMinutes;
    }

    public void setTimeToFailureMinutes(Integer timeToFailureMinutes) {
        this.timeToFailureMinutes = timeToFailureMinutes;
    }

    public String getLastPredictionTime() {
        return lastPredictionTime;
    }

    public void setLastPredictionTime(String lastPredictionTime) {
        this.lastPredictionTime = lastPredictionTime;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public Double getVibrationLevel() {
        return vibrationLevel;
    }

    public void setVibrationLevel(Double vibrationLevel) {
        this.vibrationLevel = vibrationLevel;
    }

    public Double getSoundLevel() {
        return soundLevel;
    }

    public void setSoundLevel(Double soundLevel) {
        this.soundLevel = soundLevel;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getPredictionMessage() {
        return predictionMessage;
    }

    public void setPredictionMessage(String predictionMessage) {
        this.predictionMessage = predictionMessage;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", machineType='" + machineType + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                ", lastSeen='" + lastSeen + '\'' +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                ", hardwareSerial='" + hardwareSerial + '\'' +
                '}';
    }
}

