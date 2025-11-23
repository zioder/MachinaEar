package MachinaEar.iam.boundaries;

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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Secured;
import MachinaEar.iam.security.JwtManager;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthenticationEndpoint {

    @Inject PhoenixIAMManager manager;
    @Inject IdentityRepository identities;
    @Inject JwtManager jwt;

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
        @Context HttpServletResponse response,
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
            
            // Set tokens as httpOnly cookies
            setTokenCookies(response, pair.accessToken(), pair.refreshToken());
            
            return Response.ok(pair).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @POST @Path("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password. Creates session and redirects to /authorize if OAuth flow, otherwise returns tokens."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Login successful (non-OAuth flow)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "302",
            description = "Login successful (OAuth flow) - redirects to /authorize"
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
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
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

            // If authentication successful, create session with identity ID
            if (result.authenticated()) {
                Identity user = identities.findByEmail(req.email)
                    .orElseThrow(() -> new SecurityException("User not found"));

                HttpSession session = request.getSession(true);
                session.setAttribute("identity_id", user.getId().toHexString());

                // Set tokens as httpOnly cookies
                if (result.tokens() != null) {
                    setTokenCookies(response, result.tokens().accessToken(), result.tokens().refreshToken());
                }

                // Check if this is part of OAuth flow
                String oauthClientId = (String) session.getAttribute("oauth_client_id");
                String oauthRedirectUri = (String) session.getAttribute("oauth_redirect_uri");
                String oauthCodeChallenge = (String) session.getAttribute("oauth_code_challenge");
                String oauthCodeChallengeMethod = (String) session.getAttribute("oauth_code_challenge_method");
                String oauthState = (String) session.getAttribute("oauth_state");
                String oauthScope = (String) session.getAttribute("oauth_scope");

                if (oauthClientId != null && oauthRedirectUri != null) {
                    // OAuth flow - redirect to /authorize to complete authorization
                    try {
                        StringBuilder authorizeUrl = new StringBuilder(request.getContextPath())
                            .append("/auth/authorize")
                            .append("?response_type=code")
                            .append("&client_id=").append(URLEncoder.encode(oauthClientId, "UTF-8"))
                            .append("&redirect_uri=").append(URLEncoder.encode(oauthRedirectUri, "UTF-8"))
                            .append("&code_challenge=").append(URLEncoder.encode(oauthCodeChallenge, "UTF-8"))
                            .append("&code_challenge_method=").append(URLEncoder.encode(oauthCodeChallengeMethod, "UTF-8"));

                        if (oauthState != null) {
                            authorizeUrl.append("&state=").append(URLEncoder.encode(oauthState, "UTF-8"));
                        }
                        if (oauthScope != null) {
                            authorizeUrl.append("&scope=").append(URLEncoder.encode(oauthScope, "UTF-8"));
                        }

                        return Response.seeOther(URI.create(authorizeUrl.toString())).build();
                    } catch (UnsupportedEncodingException e) {
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                    }
                }
            }

            // Non-OAuth flow - return tokens directly (legacy behavior)
            return Response.ok(result).build();

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
        accessCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Allow deletion in development
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // Delete cookie
        refreshCookie.setAttribute("SameSite", "Strict");
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

    /**
     * Sets access and refresh tokens as httpOnly cookies for security.
     * Protects against XSS attacks.
     */
    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Determine if we're in production (check for HTTPS or production environment variable)
        String env = System.getProperty("app.environment", System.getenv("APP_ENVIRONMENT"));
        boolean isProduction = "production".equalsIgnoreCase(env);
        // For development, allow HTTP cookies. In production, enforce HTTPS.
        boolean secureFlag = isProduction;

        // Access token cookie (30 minutes)
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);  // Prevents JavaScript access
        accessCookie.setSecure(secureFlag); // HTTPS only in production
        accessCookie.setPath("/");
        accessCookie.setMaxAge(30 * 60); // 30 minutes
        accessCookie.setAttribute("SameSite", "Strict"); // Strong CSRF protection
        response.addCookie(accessCookie);

        // Refresh token cookie (7 days)
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(secureFlag); // HTTPS only in production
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        refreshCookie.setAttribute("SameSite", "Strict"); // Strong CSRF protection
        response.addCookie(refreshCookie);
    }

    public static class SuccessResponse {
        public String message;
        public SuccessResponse(String message) { this.message = message; }
    }

    // Error response DTO
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
