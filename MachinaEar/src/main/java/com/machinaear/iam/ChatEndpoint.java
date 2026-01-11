package com.machinaear.iam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.devices.controllers.services.GeminiService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.OPTIONS;

@Path("/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Chat", description = "AI Chatbot operations for device monitoring")
// Security temporarily disabled for testing
// @Secured({"USER", "ADMIN"})
public class ChatEndpoint {

    @Inject
    GeminiService geminiService;

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

    @POST
    @Operation(summary = "Send a message to the AI chatbot", description = "Get AI-powered insights about your devices")
    public Response chat(ChatRequest request) {
        try {
            if (request == null || request.message == null || request.message.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ChatResponse("Message cannot be empty"))
                        .header("Access-Control-Allow-Origin", "http://localhost:3000")
                        .header("Access-Control-Allow-Credentials", "true")
                        .build();
            }

            // Get AI response from Gemini
            String aiResponse;
            try {
                // Try to call Gemini service without devices for now
                aiResponse = geminiService.chat(request.message, null);
            } catch (Exception e) {
                System.err.println("Error calling Gemini service: " + e.getMessage());
                // Fallback response
                aiResponse = "Je suis le chatbot MachinaEar. Le service Gemini est temporairement indisponible. " +
                        "Votre message était: \"" + request.message + "\". " +
                        "Le système sera bientôt pleinement opérationnel pour vous fournir des insights sur vos devices IoT.";
            }

            return Response.ok(new ChatResponse(aiResponse))
                    .header("Access-Control-Allow-Origin", "http://localhost:3000")
                    .header("Access-Control-Allow-Credentials", "true")
                    .build();

        } catch (Exception e) {
            System.err.println("Error in chat endpoint: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ChatResponse("Une erreur s'est produite. Veuillez réessayer."))
                    .header("Access-Control-Allow-Origin", "http://localhost:3000")
                    .header("Access-Control-Allow-Credentials", "true")
                    .build();
        }
    }

    @OPTIONS
    public Response corsOptions() {
        return Response.ok()
                .header("Access-Control-Allow-Origin", "http://localhost:3000")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Max-Age", "3600")
                .build();
    }
}
