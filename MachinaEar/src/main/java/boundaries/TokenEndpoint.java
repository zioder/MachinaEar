package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;

@Path("/auth")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Token", description = "Token refresh operations")
public class TokenEndpoint {

    @Inject PhoenixIAMManager manager;

    @POST @Path("/token")
    @Operation(
        summary = "Refresh access token",
        description = "Use a refresh token to obtain a new access token"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Token refreshed successfully"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid grant_type or missing refresh_token"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token"
        )
    })
    public Response refresh(
        @FormParam("grant_type") 
        @Parameter(description = "Must be 'refresh_token'", required = true) 
        String grantType,
        @FormParam("refresh_token") 
        @Parameter(description = "The refresh token", required = true) 
        String refreshToken
    ) {
        if (!"refresh_token".equals(grantType)) {
            throw new BadRequestException("Unsupported grant_type");
        }
        var pair = manager.refresh(refreshToken);
        return Response.ok(pair).build();
    }
}
