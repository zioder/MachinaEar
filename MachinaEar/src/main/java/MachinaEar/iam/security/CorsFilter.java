package MachinaEar.iam.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@PreMatching
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                      ContainerResponseContext responseContext) throws IOException {
        // Get the origin from the request
        String origin = requestContext.getHeaderString("Origin");
        
        // Get allowed origins from environment or use defaults
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins == null) {
            allowedOrigins = "https://localhost:3000,https://localhost:3001,https://127.0.0.1:3000";
        }
        
        // Check if origin is allowed
        if (origin != null && isOriginAllowed(origin, allowedOrigins)) {
            responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
            responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        }
        
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            responseContext.setStatus(Response.Status.OK.getStatusCode());
        }
    }

    /**
     * Checks if the origin is in the allowed list
     */
    private boolean isOriginAllowed(String origin, String allowedOrigins) {
        if (allowedOrigins == null || origin == null) {
            return false;
        }
        String[] allowed = allowedOrigins.split(",");
        for (String allowedOrigin : allowed) {
            if (origin.equals(allowedOrigin.trim())) {
                return true;
            }
        }
        return false;
    }
}
