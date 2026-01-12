package MachinaEar.iam.controllers.managers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.controllers.Role;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.controllers.repositories.GrantRepository;
import MachinaEar.iam.controllers.repositories.ClientRepository;
import MachinaEar.iam.controllers.repositories.AuthorizationCodeRepository;
import MachinaEar.iam.controllers.repositories.ScopeRepository;
import MachinaEar.iam.controllers.repositories.RefreshTokenRepository;
import MachinaEar.iam.controllers.repositories.EmailVerificationRepository;
import MachinaEar.iam.controllers.repositories.PasswordResetRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.entities.Client;
import MachinaEar.iam.entities.Scope;
import MachinaEar.iam.security.Argon2Utility;
import MachinaEar.iam.security.AuthorizationCode;
import MachinaEar.iam.security.JwtManager;
import MachinaEar.iam.security.PasswordValidator;
import MachinaEar.iam.security.TotpManager;

@ApplicationScoped
public class PhoenixIAMManager {

    @Inject IdentityRepository identities;
    @Inject GrantRepository grants;
    @Inject ClientRepository clients;
    @Inject AuthorizationCodeRepository authCodes;
    @Inject ScopeRepository scopes;
    @Inject RefreshTokenRepository refreshTokens;
    @Inject JwtManager jwt;
    @Inject TotpManager totpManager;
    @Inject GoogleOAuthManager googleOAuth;
    @Inject EmailService emailService;
    @Inject EmailVerificationRepository emailVerifications;
    @Inject PasswordResetRepository passwordResets;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public TokenPair register(String email, String username, char[] password) {
        // Check if email already exists
        if (identities.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Validate email format (basic validation)
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Validate username
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        // Validate password strength with entropy checking
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(password);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Create new identity
        Identity user = new Identity();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(Argon2Utility.hash(password));
        user.setActive(true);
        user.setEmailVerified(false); // Must verify email

        // Assign default USER role to new registrations
        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(Role.USER);
        user.setRoles(defaultRoles);

        // Save to database
        identities.create(user);

        // Send verification email
        try {
            MachinaEar.iam.entities.EmailVerification verification = new MachinaEar.iam.entities.EmailVerification(email, user.getId().toHexString());
            emailVerifications.create(verification);
            emailService.sendVerificationEmail(email, verification.getToken());
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send verification email: " + e.getMessage());
        }

        // Generate tokens and return
        String access = jwt.generateAccessToken(user, defaultRoles, 30); // 30 min
        String refresh = jwt.generateRefreshToken(user, 7);              // 7 days
        return new TokenPair(access, refresh);
    }

    /**
     * Verifies user's email with token
     */
    public boolean verifyEmail(String token) {
        var verification = emailVerifications.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (!verification.isValid()) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        Identity user = identities.findById(verification.getIdentityId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmailVerified(true);
        identities.update(user);
        emailVerifications.markAsVerified(token);

        return true;
    }

    /**
     * Requests a password reset email
     */
    public void requestPasswordReset(String email) {
        Identity user = identities.findByEmail(email).orElse(null);
        if (user == null) return; // Silent return for security

        MachinaEar.iam.entities.PasswordReset reset = new MachinaEar.iam.entities.PasswordReset(email, user.getId().toHexString());
        passwordResets.create(reset);
        emailService.sendPasswordResetEmail(email, reset.getToken());
    }

    /**
     * Resets password with token
     */
    public boolean resetPassword(String token, char[] newPassword) {
        var reset = passwordResets.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (!reset.isValid()) {
            throw new IllegalArgumentException("Token expired or already used");
        }

        // Validate password strength
        PasswordValidator.ValidationResult validation = PasswordValidator.validate(newPassword);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        Identity user = identities.findById(reset.getIdentityId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(Argon2Utility.hash(newPassword));
        identities.update(user);
        passwordResets.markAsUsed(token);

        return true;
    }

    public LoginResult login(String email, char[] password, Integer totpCode, String recoveryCode) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));
        if (!user.isActive()) throw new IllegalStateException("User disabled");
        if (!Argon2Utility.verify(user.getPasswordHash(), password))
            throw new SecurityException("Bad credentials");

        // Check if 2FA is enabled
        if (user.isTwoFactorEnabled()) {
            boolean twoFactorValid = false;

            // Try TOTP code first
            if (totpCode != null) {
                twoFactorValid = totpManager.verifyCode(user.getTwoFactorSecret(), totpCode);
            }

            // Try recovery code if TOTP failed
            if (!twoFactorValid && recoveryCode != null && !recoveryCode.trim().isEmpty()) {
                List<String> recoveryCodes = user.getRecoveryCodes();
                if (recoveryCodes != null) {
                    for (int i = 0; i < recoveryCodes.size(); i++) {
                        if (totpManager.verifyRecoveryCode(recoveryCodes.get(i), recoveryCode.trim())) {
                            twoFactorValid = true;
                            // Remove used recovery code
                            recoveryCodes.remove(i);
                            user.setRecoveryCodes(recoveryCodes);
                            identities.update(user);
                            break;
                        }
                    }
                }
            }

            if (!twoFactorValid) {
                // Return response indicating 2FA is required
                if (totpCode == null && recoveryCode == null) {
                    return new LoginResult(null, true, false);
                }
                throw new SecurityException("Invalid 2FA code");
            }
        }

        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty())
            roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));

        String access = jwt.generateAccessToken(user, roles, 30); // 30 min
        String refresh = jwt.generateRefreshToken(user, 7);       // 7 days
        return new LoginResult(new TokenPair(access, refresh), user.isTwoFactorEnabled(), true);
    }

    /**
     * Refreshes access token with OAuth 2.1 refresh token rotation.
     * The old refresh token is revoked and a new one is issued.
     */
    public TokenPair refresh(String refreshToken) {
        try {
            // Validate JWT structure and signature
            var claims = jwt.validate(refreshToken);
            if (!"refresh".equals(claims.getStringClaim("typ")))
                throw new SecurityException("Not a refresh token");
            
            // Check if token exists and is valid in database (OAuth 2.1 requirement)
            var tokenRecord = refreshTokens.findByToken(refreshToken)
                .orElseThrow(() -> new SecurityException("Refresh token not found"));
            
            if (!tokenRecord.isValid()) {
                throw new SecurityException("Refresh token is revoked or expired");
            }
            
            String subject = claims.getSubject();
            Identity user = identities.findByEmail(subject)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown subject"));

            Set<Role> roles = user.getRoles();
            if (roles == null || roles.isEmpty())
                roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));

            // Generate new tokens
            String access = jwt.generateAccessToken(user, roles, 30);
            String newRefresh = jwt.generateRefreshToken(user, 7);
            
            // OAuth 2.1: Store new refresh token and revoke old one (token rotation)
            var newTokenRecord = refreshTokens.createWithToken(
                newRefresh, 
                user.getId().toHexString(), 
                tokenRecord.getClientId(),
                Instant.now().plusSeconds(7 * 24 * 60 * 60)
            );
            refreshTokens.revokeAndReplace(refreshToken, newTokenRecord.getId().toHexString());
            
            return new TokenPair(access, newRefresh);
        } catch (Exception e) {
            throw new SecurityException("Invalid refresh token: " + e.getMessage());
        }
    }

    /**
     * Revokes a refresh token (used by the revocation endpoint).
     */
    public void revokeRefreshToken(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    /**
     * Initiates 2FA setup for a user and returns QR code and recovery codes.
     */
    public TwoFactorSetup setup2FA(String email) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));

        if (!user.isActive()) {
            throw new IllegalStateException("User disabled");
        }

        // Generate new secret
        String secret = totpManager.generateSecret();

        // Generate QR code
        String qrCodeUrl = totpManager.generateQrCodeUrl(email, secret, "MachinaEar");
        String qrCodeImage;
        try {
            qrCodeImage = totpManager.generateQrCodeImage(email, secret, "MachinaEar");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }

        // Generate recovery codes
        List<String> recoveryCodes = totpManager.generateRecoveryCodes(10);

        // Don't save yet - user must verify first
        return new TwoFactorSetup(secret, qrCodeUrl, qrCodeImage, recoveryCodes);
    }

    /**
     * Verifies and enables 2FA after user confirms they can generate valid codes.
     */
    public boolean enable2FA(String email, String secret, int verificationCode,
                            List<String> recoveryCodes) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));

        // Verify the code
        if (!totpManager.verifyCode(secret, verificationCode)) {
            return false;
        }

        // Hash recovery codes before storing
        List<String> hashedCodes = recoveryCodes.stream()
                .map(totpManager::hashRecoveryCode)
                .collect(Collectors.toList());

        // Save 2FA settings
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(secret);
        user.setRecoveryCodes(hashedCodes);
        identities.update(user);

        return true;
    }

    /**
     * Disables 2FA for a user.
     */
    public void disable2FA(String email, char[] password) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));

        // Verify password before disabling 2FA
        if (!Argon2Utility.verify(user.getPasswordHash(), password)) {
            throw new SecurityException("Invalid password");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setRecoveryCodes(new ArrayList<>());
        identities.update(user);
    }

    /**
     * Generates new recovery codes for a user (invalidates old ones).
     */
    public List<String> regenerateRecoveryCodes(String email, char[] password) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));

        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("2FA is not enabled");
        }

        // Verify password
        if (!Argon2Utility.verify(user.getPasswordHash(), password)) {
            throw new SecurityException("Invalid password");
        }

        // Generate new codes
        List<String> recoveryCodes = totpManager.generateRecoveryCodes(10);
        List<String> hashedCodes = recoveryCodes.stream()
                .map(totpManager::hashRecoveryCode)
                .collect(Collectors.toList());

        user.setRecoveryCodes(hashedCodes);
        identities.update(user);

        return recoveryCodes;
    }

    // ==================== OAuth 2.0 Authorization Code Flow with PKCE ====================

    /**
     * Creates an authorization code for OAuth 2.0 flow.
     * Called after user successfully authenticates and authorizes the client.
     *
     * @param identityId The authenticated user's identity ID
     * @param clientId The requesting client's ID
     * @param redirectUri The redirect URI to send the code to
     * @param codeChallenge The PKCE code challenge (SHA256 of verifier)
     * @param codeChallengeMethod PKCE method ("S256" or "plain")
     * @param state Optional state parameter for CSRF protection
     * @param scopesRequested Optional requested scopes as list
     * @return The generated authorization code string
     */
    public String createAuthorizationCode(String identityId, String clientId, String redirectUri,
                                         String codeChallenge, String codeChallengeMethod,
                                         String state, List<String> scopesRequested) {
        // Validate client exists
        Client client = clients.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client_id"));

        // Validate redirect URI is allowed for this client
        if (!client.isRedirectUriAllowed(redirectUri)) {
            throw new SecurityException("Invalid redirect_uri for this client");
        }

        // Enforce S256 per OAuth 2.1 (plain is no longer permitted)
        if (!"S256".equals(codeChallengeMethod)) {
            throw new IllegalArgumentException("Invalid code_challenge_method. Must be 'S256'");
        }

        // Generate cryptographically secure authorization code
        byte[] codeBytes = new byte[32];
        SECURE_RANDOM.nextBytes(codeBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);

        // Create authorization code entity
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setCode(code);
        authCode.setClientId(clientId);
        authCode.setIdentityId(identityId);
        authCode.setRedirectUri(redirectUri);
        authCode.setCodeChallenge(codeChallenge);
        authCode.setCodeChallengeMethod(codeChallengeMethod);
        authCode.setState(state);
        authCode.setScopes(scopesRequested != null ? scopesRequested : new ArrayList<>());
        authCode.setExpiresAt(Instant.now().plusSeconds(600)); // 10 minutes
        authCode.setUsed(false);

        // Save to database
        authCodes.create(authCode);

        return code;
    }

    /**
     * Exchanges an authorization code for access and refresh tokens.
     * Validates PKCE code_verifier against stored code_challenge.
     *
     * @param code The authorization code
     * @param clientId The client requesting token exchange
     * @param redirectUri Must match the redirect_uri used when creating the code
     * @param codeVerifier The PKCE code verifier
     * @return Token pair (access + refresh tokens)
     */
    public TokenPair exchangeAuthorizationCodeForTokens(String code, String clientId,
                                                       String redirectUri, String codeVerifier) {
        // Find authorization code
        AuthorizationCode authCode = authCodes.findByCode(code)
                .orElseThrow(() -> new SecurityException("Invalid authorization code"));

        // Validate authorization code
        if (authCode.isUsed()) {
            throw new SecurityException("Authorization code already used");
        }
        if (authCode.isExpired()) {
            throw new SecurityException("Authorization code expired");
        }

        // Validate client_id matches
        if (!authCode.getClientId().equals(clientId)) {
            throw new SecurityException("client_id mismatch");
        }

        // Validate redirect_uri matches
        if (!authCode.getRedirectUri().equals(redirectUri)) {
            throw new SecurityException("redirect_uri mismatch");
        }

        // Validate PKCE code_verifier
        if (!validatePkce(codeVerifier, authCode.getCodeChallenge(),
                         authCode.getCodeChallengeMethod())) {
            throw new SecurityException("Invalid code_verifier");
        }

        // Mark authorization code as used (single-use)
        authCodes.markAsUsed(code);

        // Get user identity by ID
        Identity user = identities.findById(authCode.getIdentityId())
                .orElseThrow(() -> new SecurityException("User not found"));

        // Get user roles
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));
        }

        // Get client for audience
        Client client = clients.findByClientId(clientId).orElse(null);
        String audience = (client != null) ? client.getAudience() : null;

        // Get scopes from authorization code
        List<String> scopesList = authCode.getScopes();

        // Generate OAuth tokens with scopes and audience
        String accessToken = jwt.generateOAuthAccessToken(user, roles, scopesList, audience, 30);
        String refreshToken = jwt.generateRefreshToken(user, 7);       // 7 days
        
        // OAuth 2.1: Store refresh token for rotation and revocation
        refreshTokens.createWithToken(
            refreshToken, 
            user.getId().toHexString(), 
            clientId,
            Instant.now().plusSeconds(7 * 24 * 60 * 60)
        );

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Validates PKCE code_verifier against code_challenge.
     *
     * @param codeVerifier The plain text verifier from client
     * @param codeChallenge The stored challenge (hash of verifier)
     * @param method "S256" (SHA-256)
     * @return true if valid, false otherwise
     */
    private boolean validatePkce(String codeVerifier, String codeChallenge, String method) {
        if (codeVerifier == null || codeChallenge == null || method == null) {
            return false;
        }

        try {
            String computedChallenge;
            if ("S256".equals(method)) {
                // Compute SHA-256 hash of code_verifier
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                computedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } else {
                return false;
            }

            return computedChallenge.equals(codeChallenge);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Registers a new OAuth 2.0 client application.
     * This should be called by administrators to register allowed clients.
     *
     * @param clientId Unique client identifier
     * @param clientName Human-readable name
     * @param redirectUris List of allowed redirect URIs
     * @return The created client
     */
    public Client registerClient(String clientId, String clientName, List<String> redirectUris) {
        // Check if client already exists
        if (clients.clientIdExists(clientId)) {
            throw new IllegalArgumentException("Client ID already exists");
        }

        Client client = new Client();
        client.setClientId(clientId);
        client.setClientName(clientName);
        client.setClientType("public"); // For SPAs, always public
        client.setRedirectUris(redirectUris);
        client.setActive(true);

        return clients.create(client);
    }

    // ==================== OAuth 2.0 Scope Management ====================

    /**
     * Register a new OAuth scope
     */
    public Scope registerScope(String name, String description) {
        if (scopes.scopeExists(name)) {
            throw new IllegalArgumentException("Scope already exists");
        }

        Scope scope = new Scope();
        scope.setName(name);
        scope.setDescription(description);
        scope.setActive(true);

        return scopes.create(scope);
    }

    /**
     * Get identity by email (for OAuth flow)
     */
    public Identity getIdentityByEmail(String email) {
        return identities.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Validate and parse requested scopes
     * Ensures scopes exist in the system and client is authorized to request them
     */
    public List<String> validateScopes(String scopeParam, Client client) {
        if (scopeParam == null || scopeParam.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> requestedScopes = Arrays.asList(scopeParam.split(" "));

        // Validate scopes exist and are active
        List<Scope> scopeEntities = scopes.findByNames(requestedScopes);
        Set<String> validScopeNames = scopeEntities.stream()
                .filter(Scope::isActive)
                .map(Scope::getName)
                .collect(Collectors.toSet());

        // Check all requested scopes are valid
        for (String requested : requestedScopes) {
            if (!validScopeNames.contains(requested)) {
                throw new IllegalArgumentException("Invalid scope: " + requested);
            }
        }

        // Check client is allowed these scopes
        if (!client.areScopesAllowed(requestedScopes)) {
            throw new SecurityException("Client not authorized for requested scopes");
        }

        return requestedScopes;
    }

    // ==================== DTOs (Java 17 records) ====================
    public static record TokenPair(String accessToken, String refreshToken) {}

    public static record LoginResult(TokenPair tokens, boolean twoFactorEnabled,
                                    boolean authenticated) {}

    public static record TwoFactorSetup(String secret, String qrCodeUrl, String qrCodeImage,
                                       List<String> recoveryCodes) {}

    public static record GoogleLoginResult(TokenPair tokens, Identity user) {}

    // ==================== Google OAuth Authentication ====================

    /**
     * Authenticate user with Google OAuth.
     * Exchanges Google authorization code for user info, finds or creates account,
     * and generates access/refresh tokens.
     *
     * @param googleAuthCode Authorization code from Google callback
     * @return GoogleLoginResult (tokens + user identity)
     * @throws IOException if Google API call fails
     * @throws SecurityException if token validation fails
     */
    public GoogleLoginResult loginWithGoogle(String googleAuthCode) {
        try {
            // 1. Exchange code for Google user info
            GoogleOAuthManager.GoogleUserInfo googleUser = googleOAuth.exchangeCodeForUserInfo(googleAuthCode);

            // 2. Find or create Identity (handles account linking)
            Identity user = googleOAuth.findOrCreateIdentity(googleUser);

            // 3. Ensure emailVerified=true for Google users (redundant but safe)
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                identities.update(user);
            }

            // 4. Get user roles
            Set<Role> roles = user.getRoles();
            if (roles == null || roles.isEmpty()) {
                roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));
            }

            // 5. Generate OAuth tokens
            String accessToken = jwt.generateAccessToken(user, roles, 30);  // 30 minutes
            String refreshToken = jwt.generateRefreshToken(user, 7);        // 7 days

            return new GoogleLoginResult(new TokenPair(accessToken, refreshToken), user);

        } catch (IOException | java.security.GeneralSecurityException e) {
            throw new SecurityException("Failed to exchange Google authorization code: " + e.getMessage(), e);
        }
    }
}
