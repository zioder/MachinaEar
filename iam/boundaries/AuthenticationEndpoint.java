package MachinaEar.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthenticationEndpoint {

    @Inject PhoenixIAMManager manager;

    public static class LoginRequest {
        public String email;
        public String password; // transmis via TLS
    }

    @POST @Path("/login")
    public Response login(LoginRequest req) {
        if (req == null || req.email == null || req.password == null) {
            throw new BadRequestException("email/password required");
        }
        var pair = manager.login(req.email, req.password.toCharArray());
        return Response.ok(pair).build();
    }
}
