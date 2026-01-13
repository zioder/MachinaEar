package MachinaEar.iam.boundaries;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.security.AltchaManager;

/**
 * ALTCHA Endpoint
 * 
 * Provides challenge generation for the ALTCHA proof-of-work
 * anti-automation protection on authentication forms.
 */
@Path("/altcha")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Security", description = "Security and anti-automation endpoints")
public class AltchaEndpoint {

    @Inject
    AltchaManager altchaManager;

    @GET
    @Path("/challenge")
    @PermitAll
    @Operation(
        summary = "Generate ALTCHA challenge",
        description = "Generates a new ALTCHA proof-of-work challenge for form protection"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Challenge generated successfully"
        )
    })
    public Response getChallenge() {
        var challenge = altchaManager.generateChallenge();
        return Response.ok(challenge.toString()).build();
    }

    @POST
    @Path("/verify")
    @PermitAll
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Verify ALTCHA solution",
        description = "Verifies an ALTCHA proof-of-work solution (used for testing)"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification result"
        )
    })
    public Response verify(String payload) {
        boolean valid = altchaManager.verify(payload);
        return Response.ok("{\"valid\":" + valid + "}").build();
    }
}
