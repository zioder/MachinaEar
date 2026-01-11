package com.machinaear.iam;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * CORS filter to allow cross-origin requests from the frontend
 */
@Provider
@ApplicationScoped
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {

        // Allow requests from localhost:3001 (frontend)
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:3001");

        // Allow credentials (cookies, authorization headers)
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");

        // Allow common HTTP methods
        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");

        // Allow common headers
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization, X-Requested-With");

        // Cache preflight requests for 1 hour
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
    }
}
