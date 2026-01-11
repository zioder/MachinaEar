package MachinaEar.devices.boundaries;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.devices.controllers.managers.DeviceManager;
import MachinaEar.devices.controllers.services.GeminiService;
import MachinaEar.devices.entities.Device;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Secured;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Chat", description = "AI Chatbot operations for device monitoring")
// Temporarily disable security for testing
// @Secured({"USER", "ADMIN"})
public class ChatEndpoint {

    @Inject
    DeviceManager deviceManager;

    @Inject
    GeminiService geminiService;

    @Inject
    IdentityRepository identities;

    public static class ChatRequest {
        public String message;
    }

    public static class ChatResponse {
        public String response;
        public long timestamp;

        public ChatResponse(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private Identity getCurrentUser(SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        return identities.findByEmail(email)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.UNAUTHORIZED));
    }

    @POST
    @Operation(summary = "Chat with AI assistant", description = "Send a message to the AI chatbot to get insights about your devices")
    public Response chat(@Context SecurityContext securityContext, ChatRequest request) {
        if (request.message == null || request.message.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Message cannot be empty")
                    .build();
        }

        try {
            // Récupérer l'utilisateur actuel
            Identity user = getCurrentUser(securityContext);

            // Récupérer tous les devices de l'utilisateur
            List<Device> devices = deviceManager.getDevices(user.getId());

            // Envoyer la requête à Gemini avec le contexte des devices
            String aiResponse = geminiService.chat(request.message, devices);

            return Response.ok(new ChatResponse(aiResponse)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ChatResponse("Erreur lors du traitement de votre demande: " + e.getMessage()))
                    .build();
        }
    }
}
