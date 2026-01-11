package MachinaEar.iam.controllers.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import MachinaEar.iam.entities.ChatConversation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatConversationRepository {

    private MongoCollection<ChatConversation> col;

    // No-args constructor for CDI proxy
    public ChatConversationRepository() {
    }

    @Inject
    public ChatConversationRepository(MongoDatabase db) {
        this.col = db.getCollection("chat_conversations", ChatConversation.class);
    }

    /**
     * Save or update a conversation
     */
    public void save(ChatConversation conversation) {
        if (conversation.getId() == null) {
            col.insertOne(conversation);
        } else {
            col.replaceOne(Filters.eq("_id", conversation.getId()), conversation);
        }
    }

    /**
     * Find all conversations for a specific user
     */
    public List<ChatConversation> findByUserId(ObjectId userId) {
        return col.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("lastActivityAt"))
                .into(new java.util.ArrayList<>());
    }

    /**
     * Find active conversations for a specific user
     */
    public List<ChatConversation> findActiveByUserId(ObjectId userId) {
        Bson filter = Filters.and(
                Filters.eq("userId", userId),
                Filters.eq("active", true));
        return col.find(filter)
                .sort(Sorts.descending("lastActivityAt"))
                .into(new java.util.ArrayList<>());
    }

    /**
     * Find a specific conversation by ID and user ID (for security)
     */
    public Optional<ChatConversation> findByIdAndUserId(ObjectId conversationId, ObjectId userId) {
        Bson filter = Filters.and(
                Filters.eq("_id", conversationId),
                Filters.eq("userId", userId));
        ChatConversation conversation = col.find(filter).first();
        return Optional.ofNullable(conversation);
    }

    /**
     * Find by ID (any user - used for admin purposes)
     */
    public Optional<ChatConversation> findById(ObjectId id) {
        ChatConversation conversation = col.find(Filters.eq("_id", id)).first();
        return Optional.ofNullable(conversation);
    }

    /**
     * Create a new conversation
     */
    public ChatConversation createConversation(ObjectId userId, String title) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title);
        conversation.setLastActivityAt(Instant.now());
        conversation.setActive(true);

        save(conversation);
        return conversation;
    }

    /**
     * Archive (soft delete) a conversation
     */
    public void archiveConversation(ObjectId conversationId, ObjectId userId) {
        Optional<ChatConversation> opt = findByIdAndUserId(conversationId, userId);
        if (opt.isPresent()) {
            ChatConversation conversation = opt.get();
            conversation.setActive(false);
            conversation.touch();
            save(conversation);
        }
    }

    /**
     * Delete archived conversations older than specified days
     */
    public long deleteOldArchivedConversations(int daysOld) {
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 60L * 60L);

        Bson filter = Filters.and(
                Filters.eq("active", false),
                Filters.lt("updatedAt", cutoffDate));

        return col.deleteMany(filter).getDeletedCount();
    }
}
