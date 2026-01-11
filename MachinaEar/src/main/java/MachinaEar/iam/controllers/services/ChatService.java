package MachinaEar.iam.controllers.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;

import MachinaEar.devices.controllers.managers.DeviceManager;
import MachinaEar.devices.controllers.services.GeminiService;
import MachinaEar.devices.entities.Device;
import MachinaEar.iam.controllers.repositories.ChatConversationRepository;
import MachinaEar.iam.entities.ChatConversation;
import MachinaEar.iam.entities.Identity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service to handle chat operations with conversation history
 */
@ApplicationScoped
public class ChatService {

    @Inject
    ChatConversationRepository conversationRepository;

    @Inject
    GeminiService geminiService;

    @Inject
    DeviceManager deviceManager;

    /**
     * Send a message and get a response, with conversation history
     * 
     * @param userId         The user sending the message
     * @param conversationId Optional conversation ID to continue existing
     *                       conversation
     * @param message        The user's message
     * @return Response object containing the AI response and conversation ID
     */
    public ChatResponse sendMessage(ObjectId userId, ObjectId conversationId, String message) {
        ChatConversation conversation;

        // Find or create conversation
        if (conversationId != null) {
            Optional<ChatConversation> existingConv = conversationRepository.findByIdAndUserId(conversationId, userId);
            if (existingConv.isPresent()) {
                conversation = existingConv.get();
            } else {
                // Invalid conversation ID for this user, create new one
                conversation = createNewConversation(userId, message);
            }
        } else {
            // Create new conversation
            conversation = createNewConversation(userId, message);
        }

        // Add user message to conversation
        conversation.addMessage("user", message);

        // Get user's devices for context
        List<Device> devices = null;
        try {
            devices = deviceManager.getDevices(userId);
        } catch (Exception e) {
            System.err.println("Error fetching devices for user " + userId + ": " + e.getMessage());
        }

        // Get AI response from Gemini
        String aiResponse;
        try {
            // Build context with conversation history
            String contextualPrompt = buildContextualPrompt(conversation, message);
            aiResponse = geminiService.chat(contextualPrompt, devices);
        } catch (Exception e) {
            System.err.println("Error calling Gemini service: " + e.getMessage());
            e.printStackTrace();
            aiResponse = "Désolé, je rencontre actuellement des difficultés techniques. " +
                    "Le service sera rétabli sous peu. Votre message a bien été enregistré.";
        }

        // Add assistant response to conversation
        conversation.addMessage("assistant", aiResponse);

        // Save updated conversation
        conversationRepository.save(conversation);

        return new ChatResponse(
                aiResponse,
                conversation.getId(),
                Instant.now());
    }

    /**
     * Get conversation history for a user
     */
    public List<ChatConversation> getUserConversations(ObjectId userId, boolean activeOnly) {
        if (activeOnly) {
            return conversationRepository.findActiveByUserId(userId);
        }
        return conversationRepository.findByUserId(userId);
    }

    /**
     * Get a specific conversation
     */
    public Optional<ChatConversation> getConversation(ObjectId conversationId, ObjectId userId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId);
    }

    /**
     * Archive a conversation
     */
    public void archiveConversation(ObjectId conversationId, ObjectId userId) {
        conversationRepository.archiveConversation(conversationId, userId);
    }

    /**
     * Create a new conversation with a title based on the first message
     */
    private ChatConversation createNewConversation(ObjectId userId, String firstMessage) {
        // Generate title from first message (max 50 chars)
        String title = generateTitle(firstMessage);
        return conversationRepository.createConversation(userId, title);
    }

    /**
     * Generate a title from the first message
     */
    private String generateTitle(String message) {
        if (message == null || message.isEmpty()) {
            return "Nouvelle conversation";
        }

        String title = message.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }

    /**
     * Build a contextual prompt including conversation history
     * (limited to last N messages to avoid token limits)
     */
    private String buildContextualPrompt(ChatConversation conversation, String currentMessage) {
        StringBuilder prompt = new StringBuilder();

        List<ChatConversation.ChatMessageEntry> messages = conversation.getMessages();

        // Include last 5 messages for context (excluding the current one we just added)
        int startIndex = Math.max(0, messages.size() - 6);
        int endIndex = messages.size() - 1; // Exclude the current message we just added

        if (startIndex < endIndex) {
            prompt.append("=== HISTORIQUE DE CONVERSATION ===\n");
            for (int i = startIndex; i < endIndex; i++) {
                ChatConversation.ChatMessageEntry msg = messages.get(i);
                prompt.append(msg.getRole().toUpperCase()).append(": ");
                prompt.append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("=== MESSAGE ACTUEL ===\n");
        prompt.append(currentMessage);

        return prompt.toString();
    }

    /**
     * Response object for chat operations
     */
    public static class ChatResponse {
        private String response;
        private ObjectId conversationId;
        private Instant timestamp;

        public ChatResponse(String response, ObjectId conversationId, Instant timestamp) {
            this.response = response;
            this.conversationId = conversationId;
            this.timestamp = timestamp;
        }

        public String getResponse() {
            return response;
        }

        public ObjectId getConversationId() {
            return conversationId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
