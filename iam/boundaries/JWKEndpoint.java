package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import MachinaEar.iam.security.JwtManager;

@Path("/.well-known")
public class JWKEndpoint {

    @Inject JwtManager jwt;

    @GET @Path("/jwks.json")
    @Produces(MediaType.APPLICATION_JSON)
    public String jwks() { return jwt.publicJwkSetJson(); }
}
