package com.machinaear.iam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simple test endpoint to verify JAX-RS is working
 */
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestEndpoint {

    @GET
    public Response test() {
        return Response.ok()
                .entity("{\"status\":\"OK\",\"message\":\"Backend is working!\"}")
                .build();
    }
}
