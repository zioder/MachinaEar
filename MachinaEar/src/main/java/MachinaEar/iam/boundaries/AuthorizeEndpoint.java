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

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.controllers.repositories.ClientRepository;
import MachinaEar.iam.entities.Client;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

@Path("/auth")
@Tag(name = "OAuth 2.0", description = "OAuth 2.0 Authorization Code Flow with PKCE")
public class AuthorizeEndpoint {

    @Inject PhoenixIAMManager manager;
    @Inject ClientRepository clients;

    @GET @Path("/authorize")
    @Produces(MediaType.TEXT_HTML)
    @Operation(
        summary = "OAuth 2.0 Authorization Endpoint",
        description = "Initiates OAuth 2.0 authorization code flow with PKCE. " +
                     "Redirects to login if user not authenticated, otherwise generates authorization code."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "302",
            description = "Redirects to login page or back to client with authorization code"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        )
    })
    public Response authorize(
        @Context HttpServletRequest request,
        @QueryParam("response_type")
        @Parameter(description = "Must be 'code'", required = true)
        String responseType,
        @QueryParam("client_id")
        @Parameter(description = "Registered client identifier", required = true)
        String clientId,
        @QueryParam("redirect_uri")
        @Parameter(description = "Callback URI for authorization code", required = true)
        String redirectUri,
        @QueryParam("code_challenge")
        @Parameter(description = "PKCE code challenge", required = true)
        String codeChallenge,
        @QueryParam("code_challenge_method")
        @Parameter(description = "PKCE method ('S256' recommended)", required = true)
        String codeChallengeMethod,
        @QueryParam("state")
        @Parameter(description = "CSRF protection token", required = false)
        String state,
        @QueryParam("scope")
        @Parameter(description = "Requested scopes", required = false)
        String scope
    ) {
        // Validate required parameters
        if (!"code".equals(responseType)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid response_type. Must be 'code'").build();
        }

        if (clientId == null || redirectUri == null || codeChallenge == null || codeChallengeMethod == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Missing required parameters: client_id, redirect_uri, code_challenge, code_challenge_method").build();
        }

        // Validate client exists
        Client client = clients.findByClientId(clientId).orElse(null);
        if (client == null || !client.isActive()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid or inactive client_id").build();
        }

        // Validate redirect_uri is allowed for this client
        if (!client.isRedirectUriAllowed(redirectUri)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid redirect_uri for this client").build();
        }

        // Check if user is authenticated (session-based)
        HttpSession session = request.getSession(false);
        String authenticatedIdentityId = null;
        if (session != null) {
            authenticatedIdentityId = (String) session.getAttribute("identity_id");
        }

        if (authenticatedIdentityId == null) {
            // User not authenticated - redirect to login page with return URL
            try {
                // Store authorization request in session for after login
                HttpSession newSession = request.getSession(true);
                newSession.setAttribute("oauth_client_id", clientId);
                newSession.setAttribute("oauth_redirect_uri", redirectUri);
                newSession.setAttribute("oauth_code_challenge", codeChallenge);
                newSession.setAttribute("oauth_code_challenge_method", codeChallengeMethod);
                newSession.setAttribute("oauth_state", state);
                newSession.setAttribute("oauth_scope", scope);

                // Redirect to Next.js login page with returnTo parameter
                // Build the return URL to continue OAuth flow after login
                StringBuilder returnToUrl = new StringBuilder("/auth/authorize")
                    .append("?response_type=code")
                    .append("&client_id=").append(URLEncoder.encode(clientId, "UTF-8"))
                    .append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"))
                    .append("&code_challenge=").append(URLEncoder.encode(codeChallenge, "UTF-8"))
                    .append("&code_challenge_method=").append(URLEncoder.encode(codeChallengeMethod, "UTF-8"));

                if (state != null) {
                    returnToUrl.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
                }
                if (scope != null) {
                    returnToUrl.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
                }

                // Redirect to Next.js login page with returnTo parameter
                String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "https://localhost:3000");
                String loginUrl = frontendUrl + "/login?returnTo=" + URLEncoder.encode(returnToUrl.toString(), "UTF-8");

                return Response.seeOther(URI.create(loginUrl)).build();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to initiate login").build();
            }
        }

        // User is authenticated - generate authorization code
        try {
            String code = manager.createAuthorizationCode(
                authenticatedIdentityId,
                clientId,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                state,
                scope
            );

            // Build redirect URL with authorization code
            String separator = redirectUri.contains("?") ? "&" : "?";
            StringBuilder redirectUrl = new StringBuilder(redirectUri)
                .append(separator)
                .append("code=").append(URLEncoder.encode(code, "UTF-8"));

            if (state != null) {
                redirectUrl.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
            }

            // Clear OAuth parameters from session
            if (session != null) {
                session.removeAttribute("oauth_client_id");
                session.removeAttribute("oauth_redirect_uri");
                session.removeAttribute("oauth_code_challenge");
                session.removeAttribute("oauth_code_challenge_method");
                session.removeAttribute("oauth_state");
                session.removeAttribute("oauth_scope");
            }

            return Response.seeOther(URI.create(redirectUrl.toString())).build();

        } catch (SecurityException | IllegalArgumentException e) {
            // Redirect to redirect_uri with error
            try {
                String separator = redirectUri.contains("?") ? "&" : "?";
                String errorUrl = redirectUri + separator + "error=invalid_request" +
                                "&error_description=" + URLEncoder.encode(e.getMessage(), "UTF-8");
                if (state != null) {
                    errorUrl += "&state=" + URLEncoder.encode(state, "UTF-8");
                }
                return Response.seeOther(URI.create(errorUrl)).build();
            } catch (UnsupportedEncodingException ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (UnsupportedEncodingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Encoding error").build();
        }
    }
}
