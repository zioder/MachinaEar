package MachinaEar.iam.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

/**
 * Entity to store chat conversation history
 */
public class ChatConversation extends RootEntity {

    // User who owns this conversation
    private ObjectId userId;

    // Conversation title (can be auto-generated from first message)
    private String title;

    // List of messages in this conversation
    private List<ChatMessageEntry> messages = new ArrayList<>();

    // Last activity timestamp
    private Instant lastActivityAt;

    // Active status
    private boolean active = true;

    public ObjectId getUserId() {
        return userId;
    }

    public void setUserId(ObjectId userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ChatMessageEntry> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageEntry> messages) {
        this.messages = messages;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Add a message to the conversation
     */
    public void addMessage(String role, String content) {
        ChatMessageEntry message = new ChatMessageEntry();
        message.setRole(role);
        message.setContent(content);
        message.setTimestamp(Instant.now());
        this.messages.add(message);
        this.lastActivityAt = Instant.now();
        this.touch();
    }

    /**
     * Inner class to represent a single message in the conversation
     */
    public static class ChatMessageEntry {
        private String role; // "user" or "assistant"
        private String content;
        private Instant timestamp;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}
