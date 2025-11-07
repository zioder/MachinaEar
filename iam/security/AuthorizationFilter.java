package MachinaEar.iam.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

// Filtre optionnel (audit/logging). Ne fait rien pour le moment.
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    @Override public void filter(ContainerRequestContext requestContext) { /* no-op */ }
}
