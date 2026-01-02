package MachinaEar.iam.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * Validates OAuth scopes from JWT claims
 * Priority is higher than AuthorizationFilter to run after authentication
 */
@Provider
@RequiresScope({})  // Empty array means this filter binds to @RequiresScope endpoints
@Priority(Priorities.AUTHORIZATION + 10)
public class ScopeValidationFilter implements ContainerRequestFilter {

    @Inject JwtManager jwt;
    @Context ResourceInfo resourceInfo;
    @Context HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        // Get the JWT token from security context (already validated by AuthorizationFilter)
        var securityContext = ctx.getSecurityContext();
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            abort(ctx, Response.Status.UNAUTHORIZED, "insufficient_scope");
            return;
        }

        // Extract JWT from Authorization header or cookie
        String token = null;
        String auth = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring("Bearer ".length()).trim();
        }

        // If not in header, try to get from httpOnly cookie
        if (token == null && request != null && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            abort(ctx, Response.Status.UNAUTHORIZED, "insufficient_scope");
            return;
        }

        try {
            JWTClaimsSet claims = jwt.validate(token);
            String scopeClaim = claims.getStringClaim("scope");

            Set<String> tokenScopes = new HashSet<>();
            if (scopeClaim != null && !scopeClaim.trim().isEmpty()) {
                tokenScopes.addAll(Arrays.asList(scopeClaim.split(" ")));
            }

            // Get required scopes from annotation
            RequiresScope ann = resourceInfo.getResourceMethod().getAnnotation(RequiresScope.class);
            if (ann == null) {
                ann = resourceInfo.getResourceClass().getAnnotation(RequiresScope.class);
            }

            if (ann != null) {
                Set<String> required = new HashSet<>(Arrays.asList(ann.value()));
                // Check if ANY required scope is present (OR logic)
                if (required.stream().noneMatch(tokenScopes::contains)) {
                    abort(ctx, Response.Status.FORBIDDEN, "insufficient_scope");
                    return;
                }
            }

        } catch (Exception e) {
            abort(ctx, Response.Status.UNAUTHORIZED, "insufficient_scope");
        }
    }

    private static void abort(ContainerRequestContext ctx, Response.Status status, String error) {
        ctx.abortWith(Response.status(status)
                .entity("{\"error\":\"" + error + "\"}")
                .build());
    }
}
