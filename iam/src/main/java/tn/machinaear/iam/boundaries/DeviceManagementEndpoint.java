package tn.machinaear.iam.boundaries;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.machinaear.iam.entities.Device;
import tn.machinaear.iam.services.DeviceService;
import tn.machinaear.iam.security.JwtManager;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API endpoint for device management
 * Provides endpoints for adding, removing, viewing, and updating devices
 */
@Path("/devices")
public class DeviceManagementEndpoint {

    private static final Logger LOGGER = Logger.getLogger(DeviceManagementEndpoint.class.getName());

    @Inject
    DeviceService deviceService;

    @EJB
    private JwtManager jwtManager;

    /**
     * Register a new device
     * POST /devices
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerDevice(@HeaderParam("Authorization") String authorizationHeader,
                                   JsonObject deviceData) {
        try {
            // Verify authorization
            String userId = extractUserIdFromToken(authorizationHeader);
            
            // Extract device data
            String deviceName = deviceData.getString("deviceName");
            String machineType = deviceData.getString("machineType");
            String location = deviceData.getString("location");
            String hardwareSerial = deviceData.getString("hardwareSerial", null);
            String firmwareVersion = deviceData.getString("firmwareVersion", null);
            String description = deviceData.getString("description", null);
            
            // Register the device
            Device device = deviceService.registerDevice(
                deviceName, machineType, location, userId, 
                hardwareSerial, firmwareVersion, description
            );
            
            JsonObject response = Json.createObjectBuilder()
                .add("id", device.getId())
                .add("deviceName", device.getDeviceName())
                .add("machineType", device.getMachineType())
                .add("location", device.getLocation())
                .add("status", device.getStatus())
                .add("registrationDate", device.getRegistrationDate())
                .add("message", "Device registered successfully")
                .build();
            
            return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
                
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Device registration failed: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during device registration: ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Json.createObjectBuilder()
                    .add("error", "An unexpected error occurred")
                    .build())
                .build();
        }
    }

    /**
     * Get all devices owned by the authenticated user
     * GET /devices/my
     */
    @GET
    @Path("/my")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyDevices(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            List<Device> devices = deviceService.getDevicesByOwnerId(userId);
            
            return Response.ok(devices).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve devices: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving devices: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get a specific device by ID
     * GET /devices/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevice(@HeaderParam("Authorization") String authorizationHeader,
                             @PathParam("id") String deviceId) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            Device device = deviceService.getDeviceById(deviceId);
            
            // Verify the user owns this device
            if (!device.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Json.createObjectBuilder()
                        .add("error", "You don't have permission to view this device")
                        .build())
                    .build();
            }
            
            return Response.ok(device).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Device not found: ", e);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving device: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Update a device
     * PUT /devices/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDevice(@HeaderParam("Authorization") String authorizationHeader,
                                @PathParam("id") String deviceId,
                                JsonObject updateData) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            Device device = deviceService.getDeviceById(deviceId);
            
            // Verify the user owns this device
            if (!device.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Json.createObjectBuilder()
                        .add("error", "You don't have permission to update this device")
                        .build())
                    .build();
            }
            
            String deviceName = updateData.getString("deviceName", null);
            String machineType = updateData.getString("machineType", null);
            String location = updateData.getString("location", null);
            String status = updateData.getString("status", null);
            String description = updateData.getString("description", null);
            
            Device updatedDevice = deviceService.updateDevice(
                deviceId, deviceName, machineType, location, status, description
            );
            
            return Response.ok(updatedDevice).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Device update failed: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating device: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Delete a device
     * DELETE /devices/{id}
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDevice(@HeaderParam("Authorization") String authorizationHeader,
                                @PathParam("id") String deviceId) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            deviceService.deleteDevice(deviceId, userId);
            
            return Response.status(Response.Status.NO_CONTENT).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Device deletion failed: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error deleting device: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get devices by status
     * GET /devices/status/{status}
     */
    @GET
    @Path("/status/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesByStatus(@HeaderParam("Authorization") String authorizationHeader,
                                       @PathParam("status") String status) {
        try {
            extractUserIdFromToken(authorizationHeader); // Verify token
            List<Device> devices = deviceService.getDevicesByStatus(status);
            
            return Response.ok(devices).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Failed to retrieve devices by status: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Update device heartbeat/last seen
     * POST /devices/{id}/heartbeat
     */
    @POST
    @Path("/{id}/heartbeat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDeviceHeartbeat(@HeaderParam("Authorization") String authorizationHeader,
                                         @PathParam("id") String deviceId) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            Device device = deviceService.getDeviceById(deviceId);
            
            // Verify the user owns this device
            if (!device.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Json.createObjectBuilder()
                        .add("error", "You don't have permission to update this device")
                        .build())
                    .build();
            }
            
            deviceService.updateDeviceLastSeen(deviceId);
            
            return Response.ok(Json.createObjectBuilder()
                .add("message", "Device heartbeat updated successfully")
                .build())
                .build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Heartbeat update failed: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    // ====== PREDICTIVE MAINTENANCE ENDPOINTS ======

    /**
     * Update device health status (from ML predictions)
     * POST /devices/{id}/health
     * 
     * This endpoint is called by:
     * - Raspberry Pi edge devices with TensorFlow Lite predictions
     * - Backend analytics service after processing sensor data
     */
    @POST
    @Path("/{id}/health")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDeviceHealth(@HeaderParam("Authorization") String authorizationHeader,
                                      @PathParam("id") String deviceId,
                                      JsonObject healthData) {
        try {
            String userId = extractUserIdFromToken(authorizationHeader);
            Device device = deviceService.getDeviceById(deviceId);
            
            // Verify ownership
            if (!device.getOwnerId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Json.createObjectBuilder()
                        .add("error", "You don't have permission to update this device")
                        .build())
                    .build();
            }
            
            // Extract health data
            String healthStatus = healthData.getString("healthStatus");
            Double anomalyScore = healthData.getJsonNumber("anomalyScore") != null ? 
                healthData.getJsonNumber("anomalyScore").doubleValue() : 0.0;
            Integer timeToFailure = healthData.containsKey("timeToFailureMinutes") && 
                !healthData.isNull("timeToFailureMinutes") ?
                healthData.getInt("timeToFailureMinutes") : null;
            Double vibrationLevel = healthData.containsKey("vibrationLevel") && 
                !healthData.isNull("vibrationLevel") ?
                healthData.getJsonNumber("vibrationLevel").doubleValue() : null;
            Double soundLevel = healthData.containsKey("soundLevel") && 
                !healthData.isNull("soundLevel") ?
                healthData.getJsonNumber("soundLevel").doubleValue() : null;
            Double temperature = healthData.containsKey("temperature") && 
                !healthData.isNull("temperature") ?
                healthData.getJsonNumber("temperature").doubleValue() : null;
            
            // Update device health
            Device updatedDevice = deviceService.updateDeviceHealthStatus(
                deviceId, healthStatus, anomalyScore, timeToFailure,
                vibrationLevel, soundLevel, temperature
            );
            
            // Return updated device with prediction
            JsonObject response = Json.createObjectBuilder()
                .add("id", updatedDevice.getId())
                .add("deviceName", updatedDevice.getDeviceName())
                .add("healthStatus", updatedDevice.getHealthStatus())
                .add("alertLevel", updatedDevice.getAlertLevel())
                .add("anomalyScore", updatedDevice.getAnomalyScore())
                .add("timeToFailureMinutes", 
                    updatedDevice.getTimeToFailureMinutes() != null ? 
                    updatedDevice.getTimeToFailureMinutes() : -1)
                .add("predictionMessage", 
                    updatedDevice.getPredictionMessage() != null ? 
                    updatedDevice.getPredictionMessage() : "No prediction available")
                .add("lastPredictionTime", updatedDevice.getLastPredictionTime())
                .build();
            
            return Response.ok(response).build();
            
        } catch (EJBException e) {
            LOGGER.log(Level.WARNING, "Health update failed: ", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                    .add("error", e.getMessage())
                    .build())
                .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get all devices with health issues (POTENTIALLY_FAULTY, CRITICAL, FAULTY)
     * GET /devices/health/issues
     */
    @GET
    @Path("/health/issues")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesWithHealthIssues(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            extractUserIdFromToken(authorizationHeader); // Verify authentication
            List<Device> devices = deviceService.getDevicesWithHealthIssues();
            return Response.ok(devices).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving devices with health issues: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get devices requiring immediate attention (CRITICAL or FAULTY)
     * GET /devices/health/critical
     */
    @GET
    @Path("/health/critical")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCriticalDevices(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            extractUserIdFromToken(authorizationHeader); // Verify authentication
            List<Device> devices = deviceService.getCriticalDevices();
            return Response.ok(devices).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving critical devices: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get devices with warnings (early detection)
     * GET /devices/health/warnings
     */
    @GET
    @Path("/health/warnings")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesWithWarnings(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            extractUserIdFromToken(authorizationHeader); // Verify authentication
            List<Device> devices = deviceService.getDevicesWithWarnings();
            return Response.ok(devices).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving devices with warnings: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Get all healthy devices (NORMAL status)
     * GET /devices/health/normal
     */
    @GET
    @Path("/health/normal")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHealthyDevices(@HeaderParam("Authorization") String authorizationHeader) {
        try {
            extractUserIdFromToken(authorizationHeader); // Verify authentication
            List<Device> devices = deviceService.getHealthyDevices();
            return Response.ok(devices).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving healthy devices: ", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Json.createObjectBuilder()
                    .add("error", "Invalid or expired token")
                    .build())
                .build();
        }
    }

    /**
     * Helper method to extract user ID from JWT token
     */
    private String extractUserIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new WebApplicationException("Authorization header missing or invalid", 
                Response.Status.UNAUTHORIZED);
        }
        
        String token = authorizationHeader.substring("Bearer ".length());
        var claims = jwtManager.verifyToken(token);
        
        if (claims.isEmpty()) {
            throw new WebApplicationException("Invalid or expired token", 
                Response.Status.UNAUTHORIZED);
        }
        
        return claims.get("sub");
    }
}

