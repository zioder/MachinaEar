package MachinaEar.devices.boundaries;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.devices.controllers.managers.DeviceManager;
import MachinaEar.devices.entities.Device;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/devices")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Devices", description = "Device management operations")
@Secured({"USER", "ADMIN"})
public class DeviceEndpoint {

    @Inject
    DeviceManager manager;

    @Inject
    IdentityRepository identities;

    public static class DeviceRequest {

        public String name;
        public String type;
    }

    public static class DeviceStatusRequest {

        public String status;
        public Double temperature;
        public Double cpuUsage;
        public Double memoryUsage;
        public String lastError;
    }

    private Identity getCurrentUser(SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        return identities.findByEmail(email)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.UNAUTHORIZED));
    }

    @GET
    @Operation(summary = "List devices", description = "List all devices for the current user")
    public Response getDevices(@Context SecurityContext securityContext) {
        Identity user = getCurrentUser(securityContext);
        List<Device> devices = manager.getDevices(user.getId());
        List<DeviceDTO> deviceDTOs = devices.stream()
                .map(DeviceDTO::new)
                .collect(Collectors.toList());
        return Response.ok(deviceDTOs).build();
    }

    @POST
    @Operation(summary = "Add device", description = "Add a new device for the current user")
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Device added successfully"),
        @APIResponse(responseCode = "400", description = "Device limit reached or invalid input")
    })
    public Response addDevice(@Context SecurityContext securityContext, DeviceRequest req) {
        Identity user = getCurrentUser(securityContext);
        try {
            Device device = manager.addDevice(user.getId(), req.name, req.type);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update device", description = "Update an existing device")
    public Response updateDevice(@Context SecurityContext securityContext, @PathParam("id") String id, DeviceRequest req) {
        Identity user = getCurrentUser(securityContext);
        try {
            Device device = manager.updateDevice(user.getId(), id, req.name, req.type);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Delete device", description = "Delete a device")
    public Response deleteDevice(@Context SecurityContext securityContext, @PathParam("id") String id) {
        Identity user = getCurrentUser(securityContext);
        try {
            manager.deleteDevice(user.getId(), id);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @PATCH
    @Path("/{id}/status")
    @Operation(summary = "Update device status", description = "Update device status and metrics for real-time monitoring")
    public Response updateDeviceStatus(@Context SecurityContext securityContext, @PathParam("id") String id, DeviceStatusRequest req) {
        Identity user = getCurrentUser(securityContext);
        try {
            Device device = manager.updateDeviceStatus(user.getId(), id, req.status, req.temperature, req.cpuUsage, req.memoryUsage, req.lastError);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/available")
    @Operation(summary = "Get available devices", description = "Get a list of devices waiting to be paired")
    public Response getAvailableDevices() {
        List<Device> available = manager.getAvailableDevices();
        List<DeviceDTO> dtos = available.stream()
                .map(DeviceDTO::new)
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    public static class PairRequest {
        public String pairingCode;
        public String name;
    }

    @POST
    @Path("/pair")
    @Operation(summary = "Pair device", description = "Pair a device using its pairing code")
    public Response pairDevice(@Context SecurityContext securityContext, PairRequest req) {
        Identity user = getCurrentUser(securityContext);
        try {
            Device device = manager.pairDevice(user.getId(), req.pairingCode, req.name);
            return Response.ok(new DeviceDTO(device)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}

