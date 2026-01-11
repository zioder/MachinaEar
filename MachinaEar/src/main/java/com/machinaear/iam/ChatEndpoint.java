package com.machinaear.iam;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.controllers.services.ChatService;
import MachinaEar.iam.entities.ChatConversation;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Secured;
import jakarta.inject.Inject;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.OPTIONS;

@Path("/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Chat", description = "AI Chatbot operations with conversation history")
@Secured({ "USER", "ADMIN" })
public class ChatEndpoint {

    @Inject
    ChatService chatService;

    @Inject
    IdentityRepository identityRepository;

    /**
     * Handle CORS preflight requests
     */
    @OPTIONS
    @Path("{path:.*}")
    public Response handlePreflight() {
        return Response.ok().build();
    }

    /**
     * Request body for sending a chat message
     */
    public static class ChatRequest {
        public String message;
        @JsonbProperty("conversationId")
        public String conversationId; // Optional - to continue existing conversation
    }

    /**
     * Response body for chat messages
     */
    public static class ChatResponse {
        public String response;
        public String conversationId;
        public long timestamp;

        public ChatResponse(String response, String conversationId, long timestamp) {
            this.response = response;
            this.conversationId = conversationId;
            this.timestamp = timestamp;
        }
    }

    /**
     * Response body for conversation list
     */
    public static class ConversationSummary {
        public String id;
        public String title;
        public long lastActivityAt;
        public int messageCount;
        public boolean active;

        public ConversationSummary(ChatConversation conversation) {
            this.id = conversation.getId().toHexString();
            this.title = conversation.getTitle();
            this.lastActivityAt = conversation.getLastActivityAt().toEpochMilli();
            this.messageCount = conversation.getMessages().size();
            this.active = conversation.isActive();
        }
    }

    /**
     * Response body for conversation details
     */
    public static class ConversationDetail {
        public String id;
        public String title;
        public long lastActivityAt;
        public boolean active;
        public List<MessageDetail> messages;

        public ConversationDetail(ChatConversation conversation) {
            this.id = conversation.getId().toHexString();
            this.title = conversation.getTitle();
            this.lastActivityAt = conversation.getLastActivityAt().toEpochMilli();
            this.active = conversation.isActive();
            this.messages = conversation.getMessages().stream()
                    .map(MessageDetail::new)
                    .collect(Collectors.toList());
        }
    }

    public static class MessageDetail {
        public String role;
        public String content;
        public long timestamp;

        public MessageDetail(ChatConversation.ChatMessageEntry message) {
            this.role = message.getRole();
            this.content = message.getContent();
            this.timestamp = message.getTimestamp().toEpochMilli();
        }
    }

    /**
     * Get current user from security context
     */
    private Identity getCurrentUser(SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        return identityRepository.findByEmail(email)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.UNAUTHORIZED));
    }

    /**
     * Send a chat message and get AI response
     */
    @POST
    @Operation(summary = "Send a message to the AI chatbot", description = "Send a message and get an AI response. Optionally continue an existing conversation.")
    public Response chat(@Context SecurityContext securityContext, ChatRequest request) {
        try {
            // Validate request
            if (request == null || request.message == null || request.message.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ChatResponse("Message cannot be empty", null, System.currentTimeMillis()))
                        .build();
            }

            // Get current user
            Identity user = getCurrentUser(securityContext);

            // Parse conversation ID if provided
            ObjectId conversationId = null;
            if (request.conversationId != null && !request.conversationId.isEmpty()) {
                try {
                    conversationId = new ObjectId(request.conversationId);
                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ChatResponse("Invalid conversation ID", null, System.currentTimeMillis()))
                            .build();
                }
            }

            // Send message and get response
            ChatService.ChatResponse serviceResponse = chatService.sendMessage(
                    user.getId(),
                    conversationId,
                    request.message);

            // Build response
            String conversationIdStr = serviceResponse.getConversationId() != null
                    ? serviceResponse.getConversationId().toHexString()
                    : null;

            ChatResponse response = new ChatResponse(
                    serviceResponse.getResponse(),
                    conversationIdStr,
                    serviceResponse.getTimestamp().toEpochMilli());

            return Response.ok(response).build();

        } catch (Exception e) {
            System.err.println("Error in chat endpoint: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ChatResponse("Une erreur s'est produite. Veuillez réessayer.", null,
                            System.currentTimeMillis()))
                    .build();
        }
    }

    /**
     * Get user's conversation history
     */
    @GET
    @Path("/conversations")
    @Operation(summary = "Get conversation history", description = "Retrieve all conversations for the current user")
    public Response getConversations(
            @Context SecurityContext securityContext,
            @QueryParam("activeOnly") boolean activeOnly) {
        try {
            Identity user = getCurrentUser(securityContext);

            List<ChatConversation> conversations = chatService.getUserConversations(user.getId(), activeOnly);

            List<ConversationSummary> summaries = conversations.stream()
                    .map(ConversationSummary::new)
                    .collect(Collectors.toList());

            return Response.ok(summaries).build();

        } catch (Exception e) {
            System.err.println("Error fetching conversations: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific conversation with all messages
     */
    @GET
    @Path("/conversations/{conversationId}")
    @Operation(summary = "Get conversation details", description = "Retrieve a specific conversation with all messages")
    public Response getConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") String conversationIdStr) {
        try {
            Identity user = getCurrentUser(securityContext);

            ObjectId conversationId;
            try {
                conversationId = new ObjectId(conversationIdStr);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid conversation ID")
                        .build();
            }

            return chatService.getConversation(conversationId, user.getId())
                    .map(conv -> Response.ok(new ConversationDetail(conv)).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());

        } catch (Exception e) {
            System.err.println("Error fetching conversation: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Archive a conversation
     */
    @DELETE
    @Path("/conversations/{conversationId}")
    @Operation(summary = "Archive conversation", description = "Archive (soft delete) a conversation")
    public Response archiveConversation(
            @Context SecurityContext securityContext,
            @PathParam("conversationId") String conversationIdStr) {
        try {
            Identity user = getCurrentUser(securityContext);

            ObjectId conversationId;
            try {
                conversationId = new ObjectId(conversationIdStr);
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid conversation ID")
                        .build();
            }

            chatService.archiveConversation(conversationId, user.getId());

            return Response.noContent().build();

        } catch (Exception e) {
            System.err.println("Error archiving conversation: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
