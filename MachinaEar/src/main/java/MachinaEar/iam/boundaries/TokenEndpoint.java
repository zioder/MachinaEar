package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;

@Path("/auth")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Token", description = "OAuth 2.0 Token operations")
public class TokenEndpoint {

    @Inject PhoenixIAMManager manager;

    @POST @Path("/token")
    @Operation(
        summary = "OAuth 2.0 Token Endpoint",
        description = "Exchange authorization code for tokens (authorization_code grant) or refresh access token (refresh_token grant)"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Tokens issued successfully"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid grant_type or missing required parameters"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Invalid authorization code, code_verifier, or refresh token"
        )
    })
    public Response token(
        @Context HttpServletResponse response,
        @FormParam("grant_type")
        @Parameter(description = "'authorization_code' or 'refresh_token'", required = true)
        String grantType,

        // Authorization code grant parameters
        @FormParam("code")
        @Parameter(description = "Authorization code (required for authorization_code grant)")
        String code,
        @FormParam("client_id")
        @Parameter(description = "Client identifier (required for authorization_code grant)")
        String clientId,
        @FormParam("redirect_uri")
        @Parameter(description = "Redirect URI (required for authorization_code grant)")
        String redirectUri,
        @FormParam("code_verifier")
        @Parameter(description = "PKCE code verifier (required for authorization_code grant)")
        String codeVerifier,

        // Refresh token grant parameters
        @FormParam("refresh_token")
        @Parameter(description = "Refresh token (required for refresh_token grant)")
        String refreshToken
    ) {
        if ("authorization_code".equals(grantType)) {
            // OAuth 2.0 Authorization Code Grant with PKCE
            if (code == null || clientId == null || redirectUri == null || codeVerifier == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("invalid_request",
                           "Missing required parameters: code, client_id, redirect_uri, code_verifier"))
                    .build();
            }

            try {
                var tokenPair = manager.exchangeAuthorizationCodeForTokens(
                    code, clientId, redirectUri, codeVerifier
                );

                // Cookies disabled for cross-domain auth - tokens returned in response body
                // setTokenCookies(response, tokenPair.accessToken(), tokenPair.refreshToken());

                // Also return tokens in response body for compatibility
                return Response.ok(tokenPair).build();

            } catch (SecurityException | IllegalArgumentException e) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("invalid_grant", e.getMessage()))
                    .build();
            }

        } else if ("refresh_token".equals(grantType)) {
            // Refresh Token Grant
            if (refreshToken == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("invalid_request", "Missing refresh_token"))
                    .build();
            }

            try {
                var tokenPair = manager.refresh(refreshToken);

                // Cookies disabled for cross-domain auth - tokens returned in response body
                // setTokenCookies(response, tokenPair.accessToken(), tokenPair.refreshToken());

                return Response.ok(tokenPair).build();

            } catch (SecurityException e) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("invalid_grant", "Invalid or expired refresh token"))
                    .build();
            }

        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("unsupported_grant_type",
                       "grant_type must be 'authorization_code' or 'refresh_token'"))
                .build();
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

    // OAuth 2.0 error response format
    public static class ErrorResponse {
        public String error;
        public String error_description;

        public ErrorResponse(String error, String description) {
            this.error = error;
            this.error_description = description;
        }
    }
}
