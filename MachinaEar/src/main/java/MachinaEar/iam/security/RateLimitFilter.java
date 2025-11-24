package MachinaEar.iam.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import MachinaEar.iam.controllers.repositories.AuditLogRepository;
import MachinaEar.iam.entities.AuditLog;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent brute force attacks
 * Tracks failed authentication attempts per IP and per email
 *
 * Configuration:
 * - Max 5 failed attempts per email in 15 minutes
 * - Max 20 failed attempts per IP in 15 minutes
 * - Endpoints protected: /auth/login, /auth/register, /auth/token
 */
@WebFilter(urlPatterns = {"/auth/login", "/auth/register", "/auth/token"})
@Priority(1) // Execute early in the filter chain
public class RateLimitFilter implements Filter {

    // Configuration
    private static final int MAX_ATTEMPTS_PER_EMAIL = 5;
    private static final int MAX_ATTEMPTS_PER_IP = 20;
    private static final int WINDOW_MINUTES = 15;

    // In-memory cache for quick rate limit checks (expires after WINDOW_MINUTES)
    private static final Map<String, RateLimitEntry> emailLimits = new ConcurrentHashMap<>();
    private static final Map<String, RateLimitEntry> ipLimits = new ConcurrentHashMap<>();

    @Inject
    AuditLogRepository auditRepo;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ipAddress = getClientIp(httpRequest);

        // Check IP-based rate limit
        if (isRateLimited(ipAddress, ipLimits, MAX_ATTEMPTS_PER_IP)) {
            logRateLimitExceeded(ipAddress, null);
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"error\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        // Allow the request to proceed
        chain.doFilter(request, response);
    }

    /**
     * Check if rate limit is exceeded for a given key
     */
    private boolean isRateLimited(String key, Map<String, RateLimitEntry> limitsMap, int maxAttempts) {
        RateLimitEntry entry = limitsMap.get(key);
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(WINDOW_MINUTES * 60L);

        if (entry == null) {
            return false; // No history, allow
        }

        // Clean up expired entries
        if (entry.timestamp.isBefore(windowStart)) {
            limitsMap.remove(key);
            return false;
        }

        return entry.attempts >= maxAttempts;
    }

    /**
     * Record a failed attempt (called by endpoints after authentication failure)
     */
    public static void recordFailedAttempt(String ipAddress, String email) {
        Instant now = Instant.now();

        // Record IP-based attempt
        ipLimits.compute(ipAddress, (k, v) -> {
            if (v == null || v.timestamp.isBefore(now.minusSeconds(WINDOW_MINUTES * 60L))) {
                return new RateLimitEntry(now, 1);
            }
            return new RateLimitEntry(v.timestamp, v.attempts + 1);
        });

        // Record email-based attempt
        if (email != null) {
            emailLimits.compute(email, (k, v) -> {
                if (v == null || v.timestamp.isBefore(now.minusSeconds(WINDOW_MINUTES * 60L))) {
                    return new RateLimitEntry(now, 1);
                }
                return new RateLimitEntry(v.timestamp, v.attempts + 1);
            });
        }
    }

    /**
     * Get client IP address, considering X-Forwarded-For header
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, first one is the client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Log rate limit exceeded event
     */
    private void logRateLimitExceeded(String ipAddress, String email) {
        if (auditRepo != null) {
            AuditLog log = new AuditLog();
            log.setEventType(AuditLog.EventType.RATE_LIMIT_EXCEEDED);
            log.setIpAddress(ipAddress);
            log.setUserEmail(email);
            log.setSuccess(false);
            log.setDetails("Rate limit exceeded: too many failed attempts");
            auditRepo.log(log);
        }
    }

    /**
     * Internal class to track rate limit entries
     */
    private static class RateLimitEntry {
        final Instant timestamp;
        final int attempts;

        RateLimitEntry(Instant timestamp, int attempts) {
            this.timestamp = timestamp;
            this.attempts = attempts;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Cleanup task could be added here to periodically clear old entries
    }

    @Override
    public void destroy() {
        emailLimits.clear();
        ipLimits.clear();
    }
}
