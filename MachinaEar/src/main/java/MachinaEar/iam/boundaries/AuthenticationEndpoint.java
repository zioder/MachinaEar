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
        public Integer totpCode; // 6-digit TOTP code (optional)
        public String recoveryCode; // Recovery code for 2FA (optional)
    }

    public static class RegisterRequest {
        public String email;
        public String username;
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
        if (req == null || req.email == null || req.username == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("email/username/password required")).build();
        }
        try {
            var pair = manager.register(req.email, req.username, req.password.toCharArray());
            return Response.ok(pair).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
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
        try {
            var result = manager.login(req.email, req.password.toCharArray(),
                req.totpCode, req.recoveryCode);
            return Response.ok(result).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // Error response DTO
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
