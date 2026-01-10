package MachinaEar.iam.controllers.managers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import MachinaEar.iam.controllers.Role;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.entities.Identity;

/**
 * Manages Google OAuth 2.0 authentication flow.
 * Handles authorization URL generation, token exchange, and user account linking.
 *
 * Configuration via environment variables:
 * - GOOGLE_OAUTH_CLIENT_ID (required)
 * - GOOGLE_OAUTH_CLIENT_SECRET (required)
 * - GOOGLE_OAUTH_REDIRECT_URI (optional, defaults to localhost for dev)
 */
@ApplicationScoped
public class GoogleOAuthManager {

    private static final Logger LOGGER = Logger.getLogger(GoogleOAuthManager.class.getName());

    @Inject IdentityRepository identities;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private boolean enabled;

    private static final String GOOGLE_AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @PostConstruct
    public void init() {
        // Load configuration from environment variables
        this.clientId = System.getenv("GOOGLE_OAUTH_CLIENT_ID");
        this.clientSecret = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET");
        this.redirectUri = getEnvOrDefault("GOOGLE_OAUTH_REDIRECT_URI",
                "http://localhost:8080/iam-0.1.0/iam/auth/google/callback");

        // Check if Google OAuth is enabled
        this.enabled = (clientId != null && !clientId.isBlank() &&
                        clientSecret != null && !clientSecret.isBlank());

        if (!enabled) {
            LOGGER.warning("GOOGLE_OAUTH_CLIENT_ID or GOOGLE_OAUTH_CLIENT_SECRET not set. " +
                          "Google OAuth is DISABLED. Set these environment variables to enable.");
        } else {
            LOGGER.info("Google OAuth enabled with redirect URI: " + redirectUri);
        }
    }

    /**
     * Build Google authorization URL for user consent screen.
     * Includes scopes: openid, email, profile
     *
     * @param stateToken CSRF protection token
     * @return Google OAuth authorization URL
     */
    public String buildAuthorizationUrl(String stateToken) {
        if (!enabled) {
            throw new IllegalStateException("Google OAuth is not configured");
        }

        try {
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientId, clientSecret,
                    Arrays.asList("openid", "email", "profile"))
                    .build();

            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(stateToken)
                    .setAccessType("online") // Don't need offline access
                    .build();

            return authUrl;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to build Google authorization URL", e);
            throw new RuntimeException("Failed to build Google authorization URL", e);
        }
    }

    /**
     * Exchange authorization code for Google user information.
     * Validates ID token and extracts user claims.
     *
     * @param authorizationCode Code from Google callback
     * @return GoogleUserInfo with user details
     * @throws IOException if token exchange fails
     * @throws GeneralSecurityException if token verification fails
     * @throws SecurityException if token validation fails
     */
    public GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode) throws IOException, GeneralSecurityException {
        if (!enabled) {
            throw new IllegalStateException("Google OAuth is not configured");
        }

        // Exchange authorization code for tokens
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                HTTP_TRANSPORT, JSON_FACTORY, GOOGLE_TOKEN_URL,
                clientId, clientSecret, authorizationCode, redirectUri)
                .execute();

        // Parse and verify ID token
        GoogleIdToken idToken = tokenResponse.parseIdToken();

        // Verify token signature and claims
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(HTTP_TRANSPORT, JSON_FACTORY)
                .setAudience(Arrays.asList(clientId))
                .build();

        if (!verifier.verify(idToken)) {
            throw new SecurityException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Extract user information
        String sub = payload.getSubject(); // Google's unique user ID
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        // Verify email is verified by Google
        if (emailVerified == null || !emailVerified) {
            throw new SecurityException("Email not verified by Google");
        }

        LOGGER.info("Google OAuth successful for user: " + email);

        return new GoogleUserInfo(sub, email, emailVerified, name, picture);
    }

    /**
     * Find or create Identity from Google user information.
     * Handles account linking: if email exists, link accounts.
     *
     * Account Linking Logic:
     * 1. Check if oauthProviderId exists → returning Google user
     * 2. Check if email exists → link to existing account, keep passwordHash
     * 3. Otherwise → create new user with oauthProvider="google"
     * 4. Always set emailVerified=true for Google users
     *
     * @param googleUser Google user information from ID token
     * @return Identity (existing or newly created)
     */
    public Identity findOrCreateIdentity(GoogleUserInfo googleUser) {
        // 1. Check by OAuth provider ID (most reliable identifier)
        Optional<Identity> byProviderId = identities.findByOAuthProvider("google", googleUser.sub());

        if (byProviderId.isPresent()) {
            // Returning Google user - update sync time and email if changed
            Identity user = byProviderId.get();
            user.setLastOAuthSync(Instant.now());

            // Update email if changed in Google account
            if (!user.getEmail().equals(googleUser.email())) {
                LOGGER.info("Updating email for Google user " + googleUser.sub() +
                           " from " + user.getEmail() + " to " + googleUser.email());
                user.setEmail(googleUser.email());
                user.setOauthProviderEmail(googleUser.email());
            }

            identities.update(user);
            LOGGER.info("Returning Google user logged in: " + user.getEmail());
            return user;
        }

        // 2. Check by email (potential account linking)
        Optional<Identity> byEmail = identities.findByEmail(googleUser.email());

        if (byEmail.isPresent()) {
            // Link existing email/password account with Google
            Identity user = byEmail.get();

            // Check if already linked to a different provider
            if (user.getOauthProvider() != null && !user.getOauthProvider().equals("google")) {
                throw new IllegalStateException(
                        "Account already linked to " + user.getOauthProvider() +
                        ". Please sign in with " + user.getOauthProvider() + ".");
            }

            // Link to Google (hybrid account: both Google + password auth)
            LOGGER.info("Linking existing account " + user.getEmail() + " to Google OAuth");
            user.setOauthProvider("google");
            user.setOauthProviderId(googleUser.sub());
            user.setOauthProviderEmail(googleUser.email());
            user.setEmailVerified(true);  // Google verified it
            user.setLastOAuthSync(Instant.now());

            identities.update(user);
            LOGGER.info("Account linked: " + user.getEmail());
            return user;
        }

        // 3. New user - create account
        LOGGER.info("Creating new Google account for: " + googleUser.email());

        Identity newUser = new Identity();
        newUser.setEmail(googleUser.email());
        newUser.setUsername(deriveUsername(googleUser.email(), googleUser.name()));
        newUser.setOauthProvider("google");
        newUser.setOauthProviderId(googleUser.sub());
        newUser.setOauthProviderEmail(googleUser.email());
        newUser.setEmailVerified(true);  // Skip email verification for Google users
        newUser.setActive(true);
        newUser.setLastOAuthSync(Instant.now());
        newUser.setPasswordHash(null);  // No password for Google-only accounts

        // Assign default USER role
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        newUser.setRoles(roles);

        identities.create(newUser);
        LOGGER.info("New Google user created: " + newUser.getEmail());

        return newUser;
    }

    /**
     * Derive username from Google name or email.
     * Removes special characters, converts to lowercase.
     */
    private String deriveUsername(String email, String name) {
        // Try name first
        if (name != null && !name.isBlank()) {
            String cleanedName = name.toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
            if (!cleanedName.isBlank()) {
                return cleanedName.substring(0, Math.min(cleanedName.length(), 20));
            }
        }

        // Fallback to email prefix
        return email.split("@")[0];
    }

    /**
     * Check if Google OAuth is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get configured redirect URI
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Helper method to get environment variable with default
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * DTO for Google user information extracted from ID token
     */
    public static record GoogleUserInfo(
            String sub,              // Google's unique user ID
            String email,            // User's email
            boolean emailVerified,   // Email verified by Google
            String name,             // Full name
            String picture           // Profile picture URL
    ) {}
}
