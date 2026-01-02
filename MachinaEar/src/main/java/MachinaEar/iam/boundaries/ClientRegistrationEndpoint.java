package MachinaEar.iam.boundaries;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.entities.Client;

import java.util.List;

@Path("/admin/clients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Client Admin", description = "Client application registration (admin only)")
public class ClientRegistrationEndpoint {

    @Inject PhoenixIAMManager manager;

    public static class RegisterClientRequest {
        public String clientId;
        public String clientName;
        public List<String> redirectUris;
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(
        summary = "Register Client Application",
        description = "Register a new client application with allowed redirect URIs. Admin only."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Client registered successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Client.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request or client ID already exists"
        ),
        @APIResponse(
            responseCode = "403",
            description = "Forbidden - admin access required"
        )
    })
    public Response registerClient(
        @RequestBody(
            description = "Client registration details",
            required = true,
            content = @Content(schema = @Schema(implementation = RegisterClientRequest.class))
        ) RegisterClientRequest req
    ) {
        if (req == null || req.clientId == null || req.clientName == null ||
            req.redirectUris == null || req.redirectUris.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("clientId, clientName, and at least one redirectUri required"))
                .build();
        }

        try {
            Client client = manager.registerClient(req.clientId, req.clientName, req.redirectUris);
            return Response.ok(client).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        }
    }
}
