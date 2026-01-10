package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.GoogleOAuthManager;
import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.controllers.repositories.GoogleOAuthStateRepository;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.GoogleOAuthState;
import MachinaEar.iam.entities.Identity;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/auth/google")
@Tag(name = "Google OAuth", description = "Google OAuth 2.0 authentication")
public class GoogleOAuthEndpoint {

    private static final Logger LOGGER = Logger.getLogger(GoogleOAuthEndpoint.class.getName());
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject GoogleOAuthManager googleOAuth;
    @Inject GoogleOAuthStateRepository stateRepo;
    @Inject PhoenixIAMManager iamManager;
    @Inject IdentityRepository identities;

    /**
     * GET /auth/google/login
     * Initiates Google OAuth flow by redirecting to Google's consent screen.
     *
     * If called during OAuth authorization code flow (with client_id, redirect_uri params),
     * stores those parameters to resume the flow after Google authentication.
     *
     * Query parameters:
     * - returnTo: Optional URL to redirect after success (for direct login)
     * - client_id, redirect_uri, code_challenge, etc.: Original OAuth params (if in OAuth flow)
     */
    @GET
    @Path("/login")
    @Operation(
        summary = "Initiate Google OAuth Login",
        description = "Redirects to Google consent screen. Stores state to resume OAuth flow after callback."
    )
    @APIResponses({
        @APIResponse(responseCode = "302", description = "Redirect to Google"),
        @APIResponse(responseCode = "503", description = "Google OAuth not configured")
    })
    public Response initiateGoogleLogin(
            @Context HttpServletRequest request,
            @QueryParam("returnTo") @Parameter(description = "URL to redirect after login") String returnTo,
            @QueryParam("client_id") @Parameter(description = "Original OAuth client_id") String clientId,
            @QueryParam("redirect_uri") @Parameter(description = "Original OAuth redirect_uri") String redirectUri,
            @QueryParam("code_challenge") @Parameter(description = "Original PKCE code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") @Parameter(description = "Original PKCE method") String codeChallengeMethod,
            @QueryParam("state") @Parameter(description = "Original OAuth state") String state,
            @QueryParam("scope") @Parameter(description = "Original OAuth scope") String scope
    ) {
        String frontendUrl = getEnvOrDefault("FRONTEND_URL", "http://localhost:3000");
        
        // Check if Google OAuth is enabled
        if (!googleOAuth.isEnabled()) {
            LOGGER.warning("Google OAuth not configured - GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET required");
            try {
                String errorMsg = "Google OAuth is not configured. Please set GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET.";
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode(errorMsg, "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException e) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=google_oauth_disabled")).build();
            }
        }

        try {
            // 1. Generate secure CSRF state token (32 bytes = 256 bits)
            byte[] stateBytes = new byte[32];
            SECURE_RANDOM.nextBytes(stateBytes);
            String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

            // 2. Store state in database with original OAuth parameters
            GoogleOAuthState oauthState = new GoogleOAuthState();
            oauthState.setStateToken(stateToken);
            oauthState.setReturnTo(returnTo);

            // Store original OAuth params if present (to resume authorization code flow)
            if (clientId != null && !clientId.isBlank()) {
                oauthState.setOriginalClientId(clientId);
                oauthState.setOriginalRedirectUri(redirectUri);
                oauthState.setOriginalCodeChallenge(codeChallenge);
                oauthState.setOriginalCodeChallengeMethod(codeChallengeMethod);
                oauthState.setOriginalState(state);
                oauthState.setOriginalScope(scope);

                LOGGER.info("Initiating Google OAuth for OAuth flow with client: " + clientId);
            } else {
                LOGGER.info("Initiating direct Google OAuth login");
            }

            stateRepo.create(oauthState);

            // 3. Build Google authorization URL
            String googleAuthUrl = googleOAuth.buildAuthorizationUrl(stateToken);

            // 4. Redirect to Google
            return Response.seeOther(URI.create(googleAuthUrl)).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initiate Google OAuth", e);
            try {
                String errorMsg = "Failed to initiate Google OAuth: " + e.getMessage();
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode(errorMsg, "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException ex) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=internal_error")).build();
            }
        }
    }

    /**
     * GET /auth/google/callback
     * Handles Google's redirect after user authenticates.
     * Validates state token, exchanges code for user info, creates/links account,
     * creates session, and either resumes OAuth flow or redirects to returnTo.
     */
    @GET
    @Path("/callback")
    @Operation(
        summary = "Google OAuth Callback",
        description = "Handles callback from Google after user authentication"
    )
    @APIResponses({
        @APIResponse(responseCode = "302", description = "Redirect to client or home"),
        @APIResponse(responseCode = "400", description = "Missing or invalid parameters"),
        @APIResponse(responseCode = "401", description = "Authentication failed")
    })
    public Response handleGoogleCallback(
            @Context HttpServletRequest request,
            @QueryParam("code") @Parameter(description = "Authorization code from Google") String code,
            @QueryParam("state") @Parameter(description = "CSRF state token") String stateToken,
            @QueryParam("error") @Parameter(description = "Error from Google") String error
    ) {
        String frontendUrl = getEnvOrDefault("FRONTEND_URL", "http://localhost:3000");
        
        // 1. Handle error from Google (user denied permission)
        if (error != null) {
            String errorMessage = switch (error) {
                case "access_denied" -> "You denied access to Google. Please try again.";
                case "invalid_scope" -> "Invalid permissions requested from Google.";
                default -> "Authentication failed: " + error;
            };

            LOGGER.warning("Google OAuth error: " + error);

            // Redirect to frontend with error
            try {
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode(errorMessage, "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException e) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=authentication_failed")).build();
            }
        }

        // 2. Validate required parameters
        if (code == null || code.isBlank() || stateToken == null || stateToken.isBlank()) {
            try {
                String errorMsg = "Missing code or state parameter";
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode(errorMsg, "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException e) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=invalid_request")).build();
            }
        }

        try {
            // 3. Validate state token (CSRF protection)
            GoogleOAuthState oauthState = stateRepo.findByStateToken(stateToken)
                    .orElseThrow(() -> new SecurityException("Invalid state token"));

            if (oauthState.isExpired()) {
                LOGGER.warning("Expired state token: " + stateToken);
                throw new SecurityException("State token expired");
            }

            if (oauthState.isUsed()) {
                LOGGER.warning("Reused state token (replay attack?): " + stateToken);
                throw new SecurityException("State token already used");
            }

            // 4. Mark state as used (prevent replay attacks)
            stateRepo.markAsUsed(stateToken);

            // 5. Authenticate with Google (exchanges code for tokens and gets/creates user)
            PhoenixIAMManager.GoogleLoginResult loginResult = iamManager.loginWithGoogle(code);
            Identity user = loginResult.user();

            // 6. Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("identity_id", user.getId().toHexString());

            LOGGER.info("Google OAuth successful for user: " + user.getEmail());

            // 8. Check if we need to resume OAuth authorization code flow
            if (oauthState.hasOriginalOAuthParams()) {
                // Resume OAuth flow by redirecting back to /auth/authorize
                String resumeUrl = buildResumeAuthorizationUrl(request, oauthState);
                LOGGER.info("Resuming OAuth flow for client: " + oauthState.getOriginalClientId());
                return Response.seeOther(URI.create(resumeUrl)).build();
            } else {
                // Direct login - redirect to returnTo or home
                String returnTo = oauthState.getReturnTo();
                if (returnTo == null || returnTo.isBlank()) {
                    returnTo = frontendUrl + "/home";
                }

                LOGGER.info("Direct Google login - redirecting to: " + returnTo);
                return Response.seeOther(URI.create(returnTo)).build();
            }

        } catch (SecurityException e) {
            LOGGER.log(Level.WARNING, "Security error during Google callback", e);
            try {
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode(e.getMessage(), "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException ex) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=authentication_failed")).build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during Google callback", e);
            try {
                String redirectUrl = frontendUrl + "/?error=" + URLEncoder.encode("Authentication failed: " + e.getMessage(), "UTF-8");
                return Response.seeOther(URI.create(redirectUrl)).build();
            } catch (UnsupportedEncodingException ex) {
                return Response.seeOther(URI.create(frontendUrl + "/?error=internal_error")).build();
            }
        }
    }

    /**
     * Build URL to resume OAuth authorization code flow after Google authentication.
     * Redirects to /auth/authorize with original parameters.
     */
    private String buildResumeAuthorizationUrl(HttpServletRequest request, GoogleOAuthState oauthState) {
        try {
            // Build absolute URL to /auth/authorize
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null) scheme = request.getScheme();

            String serverName = request.getServerName();
            int serverPort = request.getServerPort();

            if (request.getHeader("X-Forwarded-Proto") != null) {
                serverPort = "https".equalsIgnoreCase(scheme) ? 443 : 80;
            }

            String contextPath = request.getContextPath();

            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://").append(serverName);
            if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
                url.append(":").append(serverPort);
            }

            url.append(contextPath).append("/iam/auth/authorize");
            url.append("?response_type=code");
            url.append("&client_id=").append(URLEncoder.encode(oauthState.getOriginalClientId(), "UTF-8"));
            url.append("&redirect_uri=").append(URLEncoder.encode(oauthState.getOriginalRedirectUri(), "UTF-8"));
            url.append("&code_challenge=").append(URLEncoder.encode(oauthState.getOriginalCodeChallenge(), "UTF-8"));
            url.append("&code_challenge_method=").append(URLEncoder.encode(oauthState.getOriginalCodeChallengeMethod(), "UTF-8"));

            if (oauthState.getOriginalState() != null) {
                url.append("&state=").append(URLEncoder.encode(oauthState.getOriginalState(), "UTF-8"));
            }
            if (oauthState.getOriginalScope() != null) {
                url.append("&scope=").append(URLEncoder.encode(oauthState.getOriginalScope(), "UTF-8"));
            }

            return url.toString();

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to build resume URL", e);
        }
    }

    /**
     * Helper method to get environment variable with default
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    // Error response DTO
    public static class ErrorResponse {
        public String error;
        public String error_description;

        public ErrorResponse(String error, String description) {
            this.error = error;
            this.error_description = description;
        }
    }
}
