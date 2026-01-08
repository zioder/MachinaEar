package MachinaEar.devices.boundaries;

import MachinaEar.devices.controllers.managers.DeviceManager;
import MachinaEar.devices.entities.Device;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Public endpoints for device registration and pairing.
 * These endpoints are accessible without authentication to allow
 * Raspberry Pi devices to register before being paired with a user.
 */
@Path("/device-registration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Device Registration", description = "Public device registration and pairing operations")
@PermitAll
public class DeviceRegistrationEndpoint {

    @Inject
    DeviceManager manager;

    public static class RegisterPendingRequest {
        public String pairingCode;
        public String mac;
        public String hostname;
    }

    @POST
    @Path("/register-pending")
    @Operation(summary = "Register device for pairing", description = "Raspberry Pi calls this to register itself for pairing")
    public Response registerPending(RegisterPendingRequest req) {
        try {
            Device device = manager.registerPendingDevice(req.pairingCode, req.mac, req.hostname);
            return Response.accepted().entity(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/check-pairing/{pairingCode}")
    @Operation(summary = "Check pairing status", description = "Raspberry Pi polls this to check if pairing is complete")
    public Response checkPairing(@PathParam("pairingCode") String pairingCode) {
        try {
            Device device = manager.getDeviceByPairingCode(pairingCode);
            if (device.getIsPaired()) {
                return Response.ok(new DeviceDTO(device)).build();
            } else {
                return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"pending\"}").build();
            }
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    public static class StatusUpdateRequest {
        public String status;
        public Double anomalyScore;
    }

    @POST
    @Path("/status")
    @Operation(summary = "Update device status", description = "Raspberry Pi sends status updates using device token")
    public Response updateStatus(@HeaderParam("X-Device-Token") String deviceToken, StatusUpdateRequest req) {
        if (deviceToken == null || deviceToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Missing device token\"}").build();
        }
        try {
            Device device = manager.updateDeviceStatusByToken(deviceToken, req.status, req.anomalyScore);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/device-info")
    @Operation(summary = "Get device info", description = "Raspberry Pi retrieves its own info using device token")
    public Response getDeviceInfo(@HeaderParam("X-Device-Token") String deviceToken) {
        if (deviceToken == null || deviceToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Missing device token\"}").build();
        }
        try {
            Device device = manager.getDeviceByToken(deviceToken);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
