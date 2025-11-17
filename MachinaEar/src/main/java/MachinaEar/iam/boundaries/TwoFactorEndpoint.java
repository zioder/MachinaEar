package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.security.Secured;

import java.util.List;

/**
 * REST endpoint for managing two-factor authentication (2FA).
 */
@Path("/auth/2fa")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Two-Factor Authentication", description = "2FA setup and management operations")
public class TwoFactorEndpoint {

    @Inject
    PhoenixIAMManager manager;

    // Request DTOs
    public static class Setup2FARequest {
        public String email;
    }

    public static class Enable2FARequest {
        public String email;
        public String secret;
        public int verificationCode;
        public List<String> recoveryCodes;
    }

    public static class Disable2FARequest {
        public String email;
        public String password;
    }

    public static class RegenerateCodesRequest {
        public String email;
        public String password;
    }

    @POST
    @Path("/setup")
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Setup 2FA",
        description = "Initiates 2FA setup by generating a secret and QR code. Returns recovery codes."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "2FA setup initiated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - valid access token required"
        )
    })
    public Response setup2FA(
        @RequestBody(
            description = "User email",
            required = true,
            content = @Content(schema = @Schema(implementation = Setup2FARequest.class))
        ) Setup2FARequest req,
        @Context SecurityContext securityContext
    ) {
        if (req == null || req.email == null) {
            throw new BadRequestException("email required");
        }

        // Verify user is requesting their own 2FA setup
        String authenticatedUser = securityContext.getUserPrincipal().getName();
        if (!authenticatedUser.equals(req.email)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Cannot setup 2FA for another user")).build();
        }

        try {
            var setup = manager.setup2FA(req.email);
            return Response.ok(setup).build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/enable")
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Enable 2FA",
        description = "Verifies and enables 2FA after user confirms they can generate valid codes"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "2FA enabled successfully"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid verification code or request"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    public Response enable2FA(
        @RequestBody(
            description = "2FA verification details",
            required = true,
            content = @Content(schema = @Schema(implementation = Enable2FARequest.class))
        ) Enable2FARequest req,
        @Context SecurityContext securityContext
    ) {
        if (req == null || req.email == null || req.secret == null || req.recoveryCodes == null) {
            throw new BadRequestException("email, secret, verificationCode, and recoveryCodes required");
        }

        // Verify user is enabling their own 2FA
        String authenticatedUser = securityContext.getUserPrincipal().getName();
        if (!authenticatedUser.equals(req.email)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Cannot enable 2FA for another user")).build();
        }

        boolean success = manager.enable2FA(req.email, req.secret,
            req.verificationCode, req.recoveryCodes);

        if (success) {
            return Response.ok(new SuccessResponse("2FA enabled successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Invalid verification code")).build();
        }
    }

    @POST
    @Path("/disable")
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Disable 2FA",
        description = "Disables 2FA for the user after password verification"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "2FA disabled successfully"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized or invalid password"
        )
    })
    public Response disable2FA(
        @RequestBody(
            description = "User credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = Disable2FARequest.class))
        ) Disable2FARequest req,
        @Context SecurityContext securityContext
    ) {
        if (req == null || req.email == null || req.password == null) {
            throw new BadRequestException("email and password required");
        }

        // Verify user is disabling their own 2FA
        String authenticatedUser = securityContext.getUserPrincipal().getName();
        if (!authenticatedUser.equals(req.email)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Cannot disable 2FA for another user")).build();
        }

        try {
            manager.disable2FA(req.email, req.password.toCharArray());
            return Response.ok(new SuccessResponse("2FA disabled successfully")).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("Invalid password")).build();
        }
    }

    @POST
    @Path("/regenerate-codes")
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Regenerate recovery codes",
        description = "Generates new recovery codes and invalidates old ones"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Recovery codes regenerated successfully"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request or 2FA not enabled"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized or invalid password"
        )
    })
    public Response regenerateCodes(
        @RequestBody(
            description = "User credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = RegenerateCodesRequest.class))
        ) RegenerateCodesRequest req,
        @Context SecurityContext securityContext
    ) {
        if (req == null || req.email == null || req.password == null) {
            throw new BadRequestException("email and password required");
        }

        // Verify user is regenerating their own codes
        String authenticatedUser = securityContext.getUserPrincipal().getName();
        if (!authenticatedUser.equals(req.email)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Cannot regenerate codes for another user")).build();
        }

        try {
            List<String> newCodes = manager.regenerateRecoveryCodes(req.email,
                req.password.toCharArray());
            return Response.ok(new RecoveryCodesResponse(newCodes)).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("Invalid password")).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    // Response DTOs
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    public static class SuccessResponse {
        public String message;
        public SuccessResponse(String message) { this.message = message; }
    }

    public static class RecoveryCodesResponse {
        public List<String> recoveryCodes;
        public RecoveryCodesResponse(List<String> recoveryCodes) {
            this.recoveryCodes = recoveryCodes;
        }
    }
}
