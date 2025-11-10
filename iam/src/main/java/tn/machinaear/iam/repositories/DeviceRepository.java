package tn.machinaear.iam.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.machinaear.iam.entities.Device;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Repository interface for Device entity
 * Provides CRUD operations and custom queries for device management
 */
@Repository
public interface DeviceRepository extends CrudRepository<Device, String> {
    
    /**
     * Find a device by its ID
     * @param id The device ID
     * @return Optional containing the device if found
     */
    Optional<Device> findById(String id);
    
    /**
     * Find all devices owned by a specific user
     * @param ownerId The owner's identity ID
     * @return Stream of devices owned by the user
     */
    Stream<Device> findByOwnerId(String ownerId);
    
    /**
     * Find a device by its hardware serial number
     * @param hardwareSerial The hardware serial number
     * @return Optional containing the device if found
     */
    Optional<Device> findByHardwareSerial(String hardwareSerial);
    
    /**
     * Find devices by status
     * @param status The device status (ACTIVE, INACTIVE, MAINTENANCE)
     * @return Stream of devices with the specified status
     */
    Stream<Device> findByStatus(String status);
    
    /**
     * Find devices by machine type
     * @param machineType The type of machine being monitored
     * @return Stream of devices monitoring the specified machine type
     */
    Stream<Device> findByMachineType(String machineType);
    
    /**
     * Find all devices
     * @return Stream of all devices
     */
    Stream<Device> findAll();
}


