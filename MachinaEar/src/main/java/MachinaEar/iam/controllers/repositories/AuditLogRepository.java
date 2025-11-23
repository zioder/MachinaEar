package MachinaEar.iam.controllers.repositories;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.entities.AuditLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.descending;

/**
 * Repository for audit log operations
 * Provides methods to log security events and query audit trails
 */
@ApplicationScoped
public class AuditLogRepository {

    @Inject
    MongoDatabase db;

    private MongoCollection<AuditLog> collection() {
        return db.getCollection("audit_logs", AuditLog.class);
    }

    /**
     * Create a new audit log entry
     */
    public void log(AuditLog auditLog) {
        collection().insertOne(auditLog);
    }

    /**
     * Get recent failed login attempts for an email address
     * Used for rate limiting and account lockout detection
     */
    public long countFailedLoginAttempts(String email, Instant since) {
        return collection().countDocuments(
            and(
                eq("userEmail", email),
                eq("eventType", AuditLog.EventType.LOGIN_FAILURE.name()),
                gte("timestamp", since)
            )
        );
    }

    /**
     * Get recent failed attempts from an IP address
     * Used for IP-based rate limiting
     */
    public long countFailedAttemptsByIp(String ipAddress, Instant since) {
        return collection().countDocuments(
            and(
                eq("ipAddress", ipAddress),
                eq("success", false),
                gte("timestamp", since)
            )
        );
    }

    /**
     * Get audit logs for a specific user
     */
    public List<AuditLog> findByUserEmail(String email, int limit) {
        List<AuditLog> logs = new ArrayList<>();
        collection()
            .find(eq("userEmail", email))
            .sort(descending("timestamp"))
            .limit(limit)
            .into(logs);
        return logs;
    }

    /**
     * Get audit logs by event type
     */
    public List<AuditLog> findByEventType(AuditLog.EventType eventType, int limit) {
        List<AuditLog> logs = new ArrayList<>();
        collection()
            .find(eq("eventType", eventType.name()))
            .sort(descending("timestamp"))
            .limit(limit)
            .into(logs);
        return logs;
    }

    /**
     * Get recent security events (failures only)
     */
    public List<AuditLog> findRecentSecurityEvents(int hours, int limit) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        List<AuditLog> logs = new ArrayList<>();
        collection()
            .find(
                and(
                    eq("success", false),
                    gte("timestamp", since)
                )
            )
            .sort(descending("timestamp"))
            .limit(limit)
            .into(logs);
        return logs;
    }
}
