package tn.machinaear.iam.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Device entity
 */
class DeviceTest {

    @Test
    @DisplayName("Should create device with default values")
    void testDeviceDefaultConstructor() {
        // Act
        Device device = new Device();
        
        // Assert
        assertNotNull(device.getId());
        assertNotNull(device.getRegistrationDate());
        assertEquals("INACTIVE", device.getStatus());
    }

    @Test
    @DisplayName("Should create device with all parameters")
    void testDeviceParameterizedConstructor() {
        // Arrange
        String deviceName = "Server Monitor";
        String machineType = "Server";
        String location = "Data Center A";
        String ownerId = "user123";
        
        // Act
        Device device = new Device(deviceName, machineType, location, ownerId);
        
        // Assert
        assertNotNull(device.getId());
        assertEquals(deviceName, device.getDeviceName());
        assertEquals(machineType, device.getMachineType());
        assertEquals(location, device.getLocation());
        assertEquals(ownerId, device.getOwnerId());
        assertEquals("INACTIVE", device.getStatus());
        assertNotNull(device.getRegistrationDate());
    }

    @Test
    @DisplayName("Should set and get device name")
    void testSetAndGetDeviceName() {
        // Arrange
        Device device = new Device();
        String deviceName = "3D Printer Monitor";
        
        // Act
        device.setDeviceName(deviceName);
        
        // Assert
        assertEquals(deviceName, device.getDeviceName());
    }

    @Test
    @DisplayName("Should set and get machine type")
    void testSetAndGetMachineType() {
        // Arrange
        Device device = new Device();
        String machineType = "3D Printer";
        
        // Act
        device.setMachineType(machineType);
        
        // Assert
        assertEquals(machineType, device.getMachineType());
    }

    @Test
    @DisplayName("Should set and get location")
    void testSetAndGetLocation() {
        // Arrange
        Device device = new Device();
        String location = "Workshop Floor 2";
        
        // Act
        device.setLocation(location);
        
        // Assert
        assertEquals(location, device.getLocation());
    }

    @Test
    @DisplayName("Should set and get status")
    void testSetAndGetStatus() {
        // Arrange
        Device device = new Device();
        String status = "ACTIVE";
        
        // Act
        device.setStatus(status);
        
        // Assert
        assertEquals(status, device.getStatus());
    }

    @Test
    @DisplayName("Should set and get hardware serial")
    void testSetAndGetHardwareSerial() {
        // Arrange
        Device device = new Device();
        String serial = "RPI-12345-ABCDE";
        
        // Act
        device.setHardwareSerial(serial);
        
        // Assert
        assertEquals(serial, device.getHardwareSerial());
    }

    @Test
    @DisplayName("Should set and get firmware version")
    void testSetAndGetFirmwareVersion() {
        // Arrange
        Device device = new Device();
        String version = "1.0.5";
        
        // Act
        device.setFirmwareVersion(version);
        
        // Assert
        assertEquals(version, device.getFirmwareVersion());
    }

    @Test
    @DisplayName("Should set and get description")
    void testSetAndGetDescription() {
        // Arrange
        Device device = new Device();
        String description = "Monitoring main production 3D printer";
        
        // Act
        device.setDescription(description);
        
        // Assert
        assertEquals(description, device.getDescription());
    }

    @Test
    @DisplayName("Should set and get last seen")
    void testSetAndGetLastSeen() {
        // Arrange
        Device device = new Device();
        String lastSeen = "2025-11-10T10:30:00";
        
        // Act
        device.setLastSeen(lastSeen);
        
        // Assert
        assertEquals(lastSeen, device.getLastSeen());
    }

    @Test
    @DisplayName("Should generate valid toString representation")
    void testToString() {
        // Arrange
        Device device = new Device("Test Device", "Router", "Office", "user123");
        device.setHardwareSerial("RPI-001");
        
        // Act
        String toString = device.toString();
        
        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("Test Device"));
        assertTrue(toString.contains("Router"));
        assertTrue(toString.contains("Office"));
        assertTrue(toString.contains("user123"));
    }

    @Test
    @DisplayName("Should generate unique IDs for different devices")
    void testUniqueIds() {
        // Act
        Device device1 = new Device();
        Device device2 = new Device();
        
        // Assert
        assertNotNull(device1.getId());
        assertNotNull(device2.getId());
        assertNotEquals(device1.getId(), device2.getId());
    }
}


