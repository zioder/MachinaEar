package MachinaEar.iam.boundaries;

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

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthenticationEndpoint {

    @Inject PhoenixIAMManager manager;

    public static class LoginRequest {
        public String email;
        public String password; // transmis via TLS
    }

    public static class RegisterRequest {
        public String email;
        public String password; // transmis via TLS
    }

    @POST @Path("/register")
    @Operation(
        summary = "User registration",
        description = "Register a new user account with email and password"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - email or password missing/invalid, or email already exists"
        )
    })
    public Response register(
        @RequestBody(
            description = "Registration credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = RegisterRequest.class))
        ) RegisterRequest req
    ) {
        if (req == null || req.email == null || req.password == null) {
            throw new BadRequestException("email/password required");
        }
        try {
            var pair = manager.register(req.email, req.password.toCharArray());
            return Response.ok(pair).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST @Path("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password to receive access and refresh tokens"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - email or password missing"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid credentials"
        )
    })
    public Response login(
        @RequestBody(
            description = "Login credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        ) LoginRequest req
    ) {
        if (req == null || req.email == null || req.password == null) {
            throw new BadRequestException("email/password required");
        }
        var pair = manager.login(req.email, req.password.toCharArray());
        return Response.ok(pair).build();
    }
}
