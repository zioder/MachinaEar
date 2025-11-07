package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;

@Path("/auth")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public class TokenEndpoint {

    @Inject PhoenixIAMManager manager;

    @POST @Path("/token")
    public Response refresh(@FormParam("grant_type") String grantType,
                            @FormParam("refresh_token") String refreshToken) {
        if (!"refresh_token".equals(grantType)) {
            throw new BadRequestException("Unsupported grant_type");
        }
        var pair = manager.refresh(refreshToken);
        return Response.ok(pair).build();
    }
}
