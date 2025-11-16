package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.security.JwtManager;

@Path("/.well-known")
@Tag(name = "Public Keys", description = "Public key endpoints for JWT verification")
public class JWKEndpoint {

    @Inject JwtManager jwt;

    @GET @Path("/jwks.json")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get public JWK set",
        description = "Returns the JSON Web Key Set containing public keys for JWT signature verification"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "JWK set retrieved successfully"
        )
    })
    public String jwks() { return jwt.publicJwkSetJson(); }
}
