package MachinaEar.iam.security;

import java.io.IOException;
import java.security.Principal;
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

@Provider
@Secured
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationFilter implements ContainerRequestFilter {

    @Inject JwtManager jwt;
    @Context ResourceInfo resourceInfo;
    @Context HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        String token = null;

        // First, try to get token from Authorization header (for backward compatibility)
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

        // If no token found, abort
        if (token == null) {
            abort(ctx, Response.Status.UNAUTHORIZED, "Missing access token");
            return;
        }

        try {
            JWTClaimsSet claims = jwt.validate(token);
            final String subject = claims.getSubject();
            final Set<String> roles = new HashSet<>(claims.getStringListClaim("roles"));

            Secured ann = resourceInfo.getResourceMethod().getAnnotation(Secured.class);
            if (ann == null) ann = resourceInfo.getResourceClass().getAnnotation(Secured.class);
            Set<String> required = (ann == null) ? Set.of() : new HashSet<>(Arrays.asList(ann.value()));
            if (!required.isEmpty() && required.stream().noneMatch(roles::contains)) {
                abort(ctx, Response.Status.FORBIDDEN, "Insufficient role");
                return;
            }

            var base = ctx.getSecurityContext();
            ctx.setSecurityContext(new jakarta.ws.rs.core.SecurityContext() {
                @Override public Principal getUserPrincipal() { return () -> subject; }
                @Override public boolean isUserInRole(String r) { return roles.contains(r); }
                @Override public boolean isSecure() { return base != null && base.isSecure(); }
                @Override public String getAuthenticationScheme() { return "Bearer"; }
            });

        } catch (Exception e) {
            abort(ctx, Response.Status.UNAUTHORIZED, "Invalid token");
        }
    }

    private static void abort(ContainerRequestContext ctx, Response.Status status, String msg) {
        ctx.abortWith(Response.status(status)
                .entity("{\"error\":\"" + msg + "\"}")
                .build());
    }
}
