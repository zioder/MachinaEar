package tn.machinaear.iam.services;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.machinaear.iam.entities.Device;
import tn.machinaear.iam.repositories.DeviceRepository;
import tn.machinaear.iam.repositories.IdentityRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Device management
 * Handles business logic for adding, removing, and viewing devices
 */
@ApplicationScoped
public class DeviceService {

    @Inject
    private DeviceRepository deviceRepository;

    @Inject
    private IdentityRepository identityRepository;

    /**
     * Register a new device
     * @param deviceName Name of the device
     * @param machineType Type of machine being monitored (3D Printer, Router, Server, etc.)
     * @param location Physical location of the device
     * @param ownerId ID of the user who owns this device
     * @param hardwareSerial Serial number of the Raspberry Pi hardware
     * @param firmwareVersion Current firmware version
     * @param description Optional description
     * @return The created device
     */
    public Device registerDevice(String deviceName, String machineType, String location, 
                                 String ownerId, String hardwareSerial, String firmwareVersion, 
                                 String description) {
        validateDeviceInput(deviceName, machineType, location, ownerId);
        
        // Verify owner exists
        identityRepository.findById(ownerId)
            .orElseThrow(() -> new EJBException("Owner identity not found with ID: " + ownerId));
        
        // Check if hardware serial already exists
        if (hardwareSerial != null && !hardwareSerial.isEmpty()) {
            deviceRepository.findByHardwareSerial(hardwareSerial).ifPresent(device -> {
                throw new EJBException("A device with hardware serial '" + hardwareSerial + "' already exists.");
            });
        }
        
        Device device = new Device(deviceName, machineType, location, ownerId);
        device.setHardwareSerial(hardwareSerial);
        device.setFirmwareVersion(firmwareVersion);
        device.setDescription(description);
        device.setStatus("ACTIVE");
        device.setLastSeen(LocalDateTime.now().toString());
        
        deviceRepository.save(device);
        return device;
    }

    /**
     * Get a device by ID
     * @param deviceId The device ID
     * @return The device
     */
    public Device getDeviceById(String deviceId) {
        return deviceRepository.findById(deviceId)
            .orElseThrow(() -> new EJBException("Device not found with ID: " + deviceId));
    }

    /**
     * Get all devices owned by a specific user
     * @param ownerId The owner's identity ID
     * @return List of devices owned by the user
     */
    public List<Device> getDevicesByOwnerId(String ownerId) {
        // Verify owner exists
        identityRepository.findById(ownerId)
            .orElseThrow(() -> new EJBException("Owner identity not found with ID: " + ownerId));
        
        return deviceRepository.findByOwnerId(ownerId).collect(Collectors.toList());
    }

    /**
     * Get all devices (admin function)
     * @return List of all devices
     */
    public List<Device> getAllDevices() {
        return deviceRepository.findAll().collect(Collectors.toList());
    }

    /**
     * Update device information
     * @param deviceId The device ID
     * @param deviceName New device name (optional)
     * @param machineType New machine type (optional)
     * @param location New location (optional)
     * @param status New status (optional)
     * @param description New description (optional)
     * @return The updated device
     */
    public Device updateDevice(String deviceId, String deviceName, String machineType, 
                              String location, String status, String description) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new EJBException("Device not found with ID: " + deviceId));
        
        if (deviceName != null && !deviceName.isEmpty()) {
            device.setDeviceName(deviceName);
        }
        if (machineType != null && !machineType.isEmpty()) {
            device.setMachineType(machineType);
        }
        if (location != null && !location.isEmpty()) {
            device.setLocation(location);
        }
        if (status != null && !status.isEmpty()) {
            validateDeviceStatus(status);
            device.setStatus(status);
        }
        if (description != null) {
            device.setDescription(description);
        }
        
        device.setLastSeen(LocalDateTime.now().toString());
        deviceRepository.save(device);
        return device;
    }

    /**
     * Update device last seen timestamp
     * @param deviceId The device ID
     */
    public void updateDeviceLastSeen(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new EJBException("Device not found with ID: " + deviceId));
        
        device.setLastSeen(LocalDateTime.now().toString());
        deviceRepository.save(device);
    }

    /**
     * Delete a device
     * @param deviceId The device ID
     * @param requestingUserId The ID of the user requesting deletion
     */
    public void deleteDevice(String deviceId, String requestingUserId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new EJBException("Device not found with ID: " + deviceId));
        
        // Verify the requesting user is the owner
        if (!device.getOwnerId().equals(requestingUserId)) {
            throw new EJBException("Only the device owner can delete this device.");
        }
        
        deviceRepository.delete(device);
    }

    /**
     * Get devices by status
     * @param status The device status
     * @return List of devices with the specified status
     */
    public List<Device> getDevicesByStatus(String status) {
        validateDeviceStatus(status);
        return deviceRepository.findByStatus(status).collect(Collectors.toList());
    }

    /**
     * Get devices by machine type
     * @param machineType The machine type
     * @return List of devices monitoring the specified machine type
     */
    public List<Device> getDevicesByMachineType(String machineType) {
        return deviceRepository.findByMachineType(machineType).collect(Collectors.toList());
    }

    // Validation methods
    private void validateDeviceInput(String deviceName, String machineType, String location, String ownerId) {
        if (deviceName == null || deviceName.isEmpty()) {
            throw new EJBException("Device name is required.");
        }
        if (machineType == null || machineType.isEmpty()) {
            throw new EJBException("Machine type is required.");
        }
        if (location == null || location.isEmpty()) {
            throw new EJBException("Location is required.");
        }
        if (ownerId == null || ownerId.isEmpty()) {
            throw new EJBException("Owner ID is required.");
        }
    }

    private void validateDeviceStatus(String status) {
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE") && !status.equals("MAINTENANCE")) {
            throw new EJBException("Invalid device status. Must be ACTIVE, INACTIVE, or MAINTENANCE.");
        }
    }

    // ====== PREDICTIVE MAINTENANCE METHODS ======

    /**
     * Update device health status from ML model predictions
     * This is called by the edge device (Raspberry Pi) or analytics service
     * 
     * @param deviceId The device ID
     * @param healthStatus Current health status (NORMAL, WARNING, POTENTIALLY_FAULTY, CRITICAL, FAULTY)
     * @param anomalyScore ML model confidence score (0.0 to 1.0)
     * @param timeToFailureMinutes Predicted minutes until failure (null if no prediction)
     * @param vibrationLevel Current vibration reading
     * @param soundLevel Current sound level in dB
     * @param temperature Optional temperature reading
     * @return Updated device
     */
    public Device updateDeviceHealthStatus(String deviceId, String healthStatus, Double anomalyScore,
                                           Integer timeToFailureMinutes, Double vibrationLevel,
                                           Double soundLevel, Double temperature) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new EJBException("Device not found with ID: " + deviceId));
        
        // Validate health status
        validateHealthStatus(healthStatus);
        
        // Update health fields
        device.setHealthStatus(healthStatus);
        device.setAnomalyScore(anomalyScore);
        device.setTimeToFailureMinutes(timeToFailureMinutes);
        device.setVibrationLevel(vibrationLevel);
        device.setSoundLevel(soundLevel);
        device.setTemperature(temperature);
        device.setLastPredictionTime(LocalDateTime.now().toString());
        
        // Calculate predicted failure time if provided
        if (timeToFailureMinutes != null && timeToFailureMinutes > 0) {
            LocalDateTime failureTime = LocalDateTime.now().plusMinutes(timeToFailureMinutes);
            device.setPredictedFailureTime(failureTime.toString());
            
            // Set human-readable prediction message
            String message = generatePredictionMessage(healthStatus, timeToFailureMinutes);
            device.setPredictionMessage(message);
        } else {
            device.setPredictedFailureTime(null);
            device.setPredictionMessage(null);
        }
        
        // Set alert level based on health status
        String alertLevel = determineAlertLevel(healthStatus, anomalyScore);
        device.setAlertLevel(alertLevel);
        
        // Update last seen timestamp
        device.setLastSeen(LocalDateTime.now().toString());
        
        deviceRepository.save(device);
        return device;
    }

    /**
     * Get all devices with critical health issues
     * @return List of devices with POTENTIALLY_FAULTY, CRITICAL, or FAULTY status
     */
    public List<Device> getDevicesWithHealthIssues() {
        return deviceRepository.findAll()
            .filter(device -> {
                String health = device.getHealthStatus();
                return health != null && 
                       (health.equals("POTENTIALLY_FAULTY") || 
                        health.equals("CRITICAL") || 
                        health.equals("FAULTY"));
            })
            .collect(Collectors.toList());
    }

    /**
     * Get devices requiring immediate attention (CRITICAL or FAULTY)
     * @return List of devices in critical state
     */
    public List<Device> getCriticalDevices() {
        return deviceRepository.findAll()
            .filter(device -> {
                String health = device.getHealthStatus();
                return health != null && 
                       (health.equals("CRITICAL") || health.equals("FAULTY"));
            })
            .collect(Collectors.toList());
    }

    /**
     * Get devices with warnings (early detection)
     * @return List of devices with WARNING status
     */
    public List<Device> getDevicesWithWarnings() {
        return deviceRepository.findAll()
            .filter(device -> "WARNING".equals(device.getHealthStatus()))
            .collect(Collectors.toList());
    }

    /**
     * Get all healthy devices
     * @return List of devices with NORMAL status
     */
    public List<Device> getHealthyDevices() {
        return deviceRepository.findAll()
            .filter(device -> "NORMAL".equals(device.getHealthStatus()))
            .collect(Collectors.toList());
    }

    // Helper methods
    private void validateHealthStatus(String healthStatus) {
        if (healthStatus == null) {
            throw new EJBException("Health status cannot be null.");
        }
        if (!healthStatus.equals("NORMAL") && 
            !healthStatus.equals("WARNING") && 
            !healthStatus.equals("POTENTIALLY_FAULTY") && 
            !healthStatus.equals("CRITICAL") && 
            !healthStatus.equals("FAULTY")) {
            throw new EJBException("Invalid health status. Must be NORMAL, WARNING, POTENTIALLY_FAULTY, CRITICAL, or FAULTY.");
        }
    }

    private String determineAlertLevel(String healthStatus, Double anomalyScore) {
        switch (healthStatus) {
            case "NORMAL":
                return "NONE";
            case "WARNING":
                return anomalyScore != null && anomalyScore > 0.7 ? "MEDIUM" : "LOW";
            case "POTENTIALLY_FAULTY":
                return "HIGH";
            case "CRITICAL":
            case "FAULTY":
                return "CRITICAL";
            default:
                return "NONE";
        }
    }

    private String generatePredictionMessage(String healthStatus, Integer minutesToFailure) {
        if (minutesToFailure == null) {
            return null;
        }
        
        String timeDescription;
        if (minutesToFailure < 5) {
            timeDescription = "IMMEDIATE ATTENTION REQUIRED - Failure expected in less than 5 minutes";
        } else if (minutesToFailure < 15) {
            timeDescription = String.format("URGENT - Machinery showing signs of failure. Expected to fail in %d minutes", minutesToFailure);
        } else if (minutesToFailure < 60) {
            timeDescription = String.format("WARNING - Anomalous behavior detected. Potential failure in %d minutes", minutesToFailure);
        } else {
            int hours = minutesToFailure / 60;
            int mins = minutesToFailure % 60;
            timeDescription = String.format("ADVISORY - Monitoring abnormal patterns. Potential issue in %dh %dm", hours, mins);
        }
        
        return String.format("%s - Status: %s", timeDescription, healthStatus);
    }
}

