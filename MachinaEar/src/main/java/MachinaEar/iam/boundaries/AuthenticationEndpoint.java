package MachinaEar.iam.boundaries;

import jakarta.annotation.security.PermitAll;

import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Secured;
import MachinaEar.iam.security.JwtManager;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthenticationEndpoint {

    @Inject PhoenixIAMManager manager;
    @Inject IdentityRepository identities;
    @Inject JwtManager jwt;

    @GET @Path("/test")
    @PermitAll
    public Response test() {
        return Response.ok("{\"status\":\"ok\", \"message\":\"Backend updated\"}").build();
    }

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
        summary = "OAuth 2.1 User Registration - No Tokens Issued",
        description = "Register a new user account. Does not issue tokens - use OAuth 2.1 flow to authenticate after registration."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Registration successful - user created, must login via OAuth flow"
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
            // Register user only - do NOT issue tokens
            manager.register(req.email, req.username, req.password.toCharArray());

            return Response.ok(new SuccessResponse("Registration successful. Please login via OAuth flow.")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @POST @Path("/login")
    @Operation(
        summary = "OAuth 2.1 Login - Create Session Only",
        description = "Authenticate user with email and password. Creates session for OAuth flow. Use returnTo parameter to redirect after login."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Login successful - returns success message and optional redirect URL"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - email or password missing"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid credentials or 2FA required"
        )
    })
    public Response login(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        @RequestBody(
            description = "Login credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        ) LoginRequest req,
        @QueryParam("returnTo")
        @Parameter(description = "URL to redirect after successful login")
        String returnTo
    ) {
        if (req == null || req.email == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("email/password required")).build();
        }

        try {
            var result = manager.login(req.email, req.password.toCharArray(),
                req.totpCode, req.recoveryCode);

            // Check if 2FA is required
            if (result.twoFactorEnabled() && !result.authenticated()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new TwoFactorResponse("2fa_required", "Two-factor authentication code required"))
                    .build();
            }

            if (!result.authenticated()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Invalid credentials")).build();
            }

            // Authentication successful - create session ONLY (no tokens)
            Identity user = identities.findByEmail(req.email)
                .orElseThrow(() -> new SecurityException("User not found"));

            HttpSession session = request.getSession(true);
            session.setAttribute("identity_id", user.getId().toHexString());

            // Return success with optional returnTo URL for client-side redirect
            return Response.ok(new LoginSuccessResponse("Login successful", returnTo)).build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @GET @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Get current user",
        description = "Returns the currently authenticated user's information from the JWT token"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - valid access token required"
        )
    })
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        UserResponse userResponse = new UserResponse(
            user.getEmail(),
            user.getUsername(),
            user.getRoles().stream().map(Enum::name).toList()
        );

        return Response.ok(userResponse).build();
    }

    @POST @Path("/logout")
    @Operation(
        summary = "User logout",
        description = "Clears authentication cookies and invalidates the session"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Logout successful"
        )
    })
    public Response logout(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response
    ) {
        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear authentication cookies
        // Note: Secure flag doesn't matter when deleting cookies
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // Allow deletion in development
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0); // Delete cookie
        accessCookie.setAttribute("SameSite", "Lax"); // Lax for development compatibility
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Allow deletion in development
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // Delete cookie
        refreshCookie.setAttribute("SameSite", "Lax"); // Lax for development compatibility
        response.addCookie(refreshCookie);

        return Response.ok(new SuccessResponse("Logged out successfully")).build();
    }

    // Response DTOs
    public static class UserResponse {
        public String email;
        public String username;
        public java.util.List<String> roles;

        public UserResponse(String email, String username, java.util.List<String> roles) {
            this.email = email;
            this.username = username;
            this.roles = roles;
        }
    }

    public static class LoginSuccessResponse {
        public String message;
        public String returnTo;

        public LoginSuccessResponse(String message, String returnTo) {
            this.message = message;
            this.returnTo = returnTo;
        }
    }

    public static class TwoFactorResponse {
        public String error;
        public String error_description;
        public boolean twoFactorRequired = true;

        public TwoFactorResponse(String error, String description) {
            this.error = error;
            this.error_description = description;
        }
    }

    public static class SuccessResponse {
        public String message;
        public SuccessResponse(String message) { this.message = message; }
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
