package MachinaEar.iam.boundaries;

import jakarta.annotation.security.PermitAll;

import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import MachinaEar.iam.controllers.managers.PhoenixIAMManager;
import MachinaEar.iam.controllers.managers.EmailService;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.controllers.repositories.PendingRegistrationRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.entities.PendingRegistration;
import MachinaEar.iam.security.AltchaManager;
import MachinaEar.iam.security.Secured;
import MachinaEar.iam.security.JwtManager;
import MachinaEar.iam.security.Argon2Utility;
import MachinaEar.iam.security.PasswordValidator;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthenticationEndpoint {

    @Inject PhoenixIAMManager manager;
    @Inject IdentityRepository identities;
    @Inject PendingRegistrationRepository pendingRegistrations;
    @Inject EmailService emailService;
    @Inject JwtManager jwt;
    @Inject AltchaManager altchaManager;

    @GET @Path("/test")
    @PermitAll
    public Response test() {
        return Response.ok("{\"status\":\"ok\", \"message\":\"Backend updated\"}").build();
    }

    public static class LoginRequest {
        public String email;
        public String password; // transmis via TLS
        public Integer totpCode; // 6-digit TOTP code (optional)
        public String recoveryCode; // Recovery code for 2FA (optional)
        public String altcha; // ALTCHA proof-of-work response (required)
        
        public LoginRequest() {}
    }

    public static class RegisterRequest {
        public String email;
        public String username;
        public String password; // transmis via TLS
        public String altcha; // ALTCHA proof-of-work response (required)
        
        public RegisterRequest() {}
    }

    @POST @Path("/register")
    @Operation(
        summary = "Start Registration - Sends Verification Code",
        description = "Initiates registration by validating input and sending a 6-digit verification code to the email. Account is NOT created until code is verified."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Verification code sent - user must verify with /verify-code endpoint"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - email or password missing/invalid, or email already exists"
        )
    })
    public Response register(
        @RequestBody(
            description = "Registration credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = RegisterRequest.class))
        ) RegisterRequest req
    ) {
        if (req == null || req.email == null || req.username == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("email/username/password required")).build();
        }
        
        // Verify ALTCHA proof-of-work
        if (req.altcha == null || !altchaManager.verify(req.altcha)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Security verification failed. Please complete the challenge.")).build();
        }
        
        String email = req.email.trim().toLowerCase();
        String username = req.username.trim();
        char[] password = req.password.toCharArray();

        try {
            // Validate email format
            if (!email.contains("@")) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid email format")).build();
            }

            // Check if email already registered
            if (identities.emailExists(email)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Email already registered")).build();
            }

            // Validate username
            if (username.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Username is required")).build();
            }

            // Validate password strength
            PasswordValidator.ValidationResult validation = PasswordValidator.validate(password);
            if (!validation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(validation.getErrorMessage())).build();
            }

            // Hash password for storage
            String passwordHash = Argon2Utility.hash(password);

            // Create pending registration
            PendingRegistration pending = new PendingRegistration(email, username, passwordHash);
            pendingRegistrations.create(pending);

            // Send verification code email
            boolean emailSent = emailService.sendVerificationCodeEmail(email, pending.getVerificationCode());

            if (!emailSent && emailService.isEnabled()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to send verification email. Please try again.")).build();
            }

            // Return success with email masked
            String maskedEmail = maskEmail(email);
            return Response.ok(new VerificationPendingResponse(
                "Verification code sent to " + maskedEmail,
                email,
                15 // expires in 15 minutes
            )).build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    /**
     * Mask email for privacy (e.g., t***@example.com)
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    @POST @Path("/login")
    @Operation(
        summary = "OAuth 2.1 Login - Create Session Only",
        description = "Authenticate user with email and password. Creates session for OAuth flow. Use returnTo parameter to redirect after login."
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Login successful - returns success message and optional redirect URL"
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid request - email or password missing"
        ),
        @APIResponse(
            responseCode = "401",
            description = "Authentication failed - invalid credentials or 2FA required"
        )
    })
    public Response login(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response,
        @RequestBody(
            description = "Login credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class))
        ) LoginRequest req,
        @QueryParam("returnTo")
        @Parameter(description = "URL to redirect after successful login")
        String returnTo
    ) {
        if (req == null || req.email == null || req.password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("email/password required")).build();
        }

        // Verify ALTCHA proof-of-work (skip for 2FA retry - when totpCode or recoveryCode is provided)
        boolean is2FARetry = req.totpCode != null || (req.recoveryCode != null && !req.recoveryCode.isBlank());
        if (!is2FARetry && (req.altcha == null || !altchaManager.verify(req.altcha))) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Security verification failed. Please complete the challenge.")).build();
        }

        try {
            var result = manager.login(req.email, req.password.toCharArray(),
                req.totpCode, req.recoveryCode);

            // Check if 2FA is required
            if (result.twoFactorEnabled() && !result.authenticated()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new TwoFactorResponse("2fa_required", "Two-factor authentication code required"))
                    .build();
            }

            if (!result.authenticated()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Invalid credentials")).build();
            }

            // Authentication successful - create session ONLY (no tokens)
            Identity user = identities.findByEmail(req.email)
                .orElseThrow(() -> new SecurityException("User not found"));

            HttpSession session = request.getSession(true);
            session.setAttribute("identity_id", user.getId().toHexString());

            // Set session cookie with domain=".machinaear.me" to share across subdomains
            jakarta.ws.rs.core.NewCookie sessionCookie = new jakarta.ws.rs.core.NewCookie(
                "JSESSIONID", // Use default session cookie name
                session.getId(),
                "/",
                ".machinaear.me",
                "Session",
                60 * 60 * 24 * 7, // maxAge: 7 days
                true, // secure
                true  // httpOnly
            );

            // Return success with optional returnTo URL for client-side redirect
            return Response.ok(new LoginSuccessResponse("Login successful", returnTo))
                .cookie(sessionCookie)
                .build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    @POST @Path("/verify-email")
    @Operation(summary = "Verify user email (legacy)", description = "Verifies user email using the token sent via email")
    public Response verifyEmail(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Token required")).build();
        }
        try {
            manager.verifyEmail(token);
            return Response.ok(new SuccessResponse("Email verified successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    public static class VerifyCodeRequest {
        public String email;
        public String code;
        public VerifyCodeRequest() {}
    }

    @POST @Path("/verify-code")
    @Operation(
        summary = "Verify Registration Code",
        description = "Verifies the 6-digit code sent to email and completes account registration"
    )
    @APIResponses({
        @APIResponse(responseCode = "200", description = "Account created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid or expired code")
    })
    public Response verifyCode(VerifyCodeRequest req) {
        if (req == null || req.email == null || req.code == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Email and code required")).build();
        }

        String email = req.email.trim().toLowerCase();
        String code = req.code.trim();

        try {
            // Find pending registration
            PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .orElse(null);

            if (pending == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("No pending registration found. Please register again.")).build();
            }

            // Check if expired
            if (pending.isExpired()) {
                pendingRegistrations.markVerifiedAndDelete(email);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Verification code expired. Please register again.")).build();
            }

            // Check too many attempts
            if (pending.isTooManyAttempts()) {
                pendingRegistrations.markVerifiedAndDelete(email);
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Too many failed attempts. Please register again.")).build();
            }

            // Verify code
            if (!pending.verifyCode(code)) {
                pending.incrementAttempts();
                pendingRegistrations.update(pending);
                int remaining = 5 - pending.getAttempts();
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid code. " + remaining + " attempts remaining.")).build();
            }

            // Code is valid - create the actual account
            manager.createVerifiedAccount(
                pending.getEmail(),
                pending.getUsername(),
                pending.getPasswordHash()
            );

            // Clean up pending registration
            pendingRegistrations.markVerifiedAndDelete(email);

            return Response.ok(new SuccessResponse("Account created successfully! You can now sign in.")).build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    public static class ResendCodeRequest {
        public String email;
        public ResendCodeRequest() {}
    }

    @POST @Path("/resend-code")
    @Operation(
        summary = "Resend Verification Code",
        description = "Generates and sends a new verification code to the email"
    )
    public Response resendCode(ResendCodeRequest req) {
        if (req == null || req.email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Email required")).build();
        }

        String email = req.email.trim().toLowerCase();

        try {
            PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .orElse(null);

            if (pending == null) {
                // Don't reveal if email exists or not
                return Response.ok(new SuccessResponse("If a pending registration exists, a new code has been sent.")).build();
            }

            // Regenerate code
            pending.regenerateCode();
            pendingRegistrations.update(pending);

            // Send new code
            emailService.sendVerificationCodeEmail(email, pending.getVerificationCode());

            String maskedEmail = maskEmail(email);
            return Response.ok(new SuccessResponse("New verification code sent to " + maskedEmail)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Failed to resend code. Please try again.")).build();
        }
    }

    @POST @Path("/forgot-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Request password reset", description = "Sends a password reset link to the user's email")
    public Response forgotPassword(ForgotPasswordRequest req) {
        if (req == null || req.email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Email required")).build();
        }
        manager.requestPasswordReset(req.email);
        return Response.ok(new SuccessResponse("If an account exists with that email, a reset link has been sent.")).build();
    }

    @POST @Path("/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reset password", description = "Resets user password using the token sent via email")
    public Response resetPassword(@QueryParam("token") String token, ResetPasswordRequest req) {
        if (token == null || token.isBlank() || req == null || req.newPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Token and new password required")).build();
        }
        try {
            manager.resetPassword(token, req.newPassword.toCharArray());
            return Response.ok(new SuccessResponse("Password reset successfully")).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    public static class ForgotPasswordRequest {
        public String email;
        
        public ForgotPasswordRequest() {}
    }

    public static class ResetPasswordRequest {
        public String newPassword;
        
        public ResetPasswordRequest() {}
    }

    @GET @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured({"USER", "ADMIN", "MAINTAINER"})
    @Operation(
        summary = "Get current user",
        description = "Returns the currently authenticated user's information from the JWT token"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON)
        ),
        @APIResponse(
            responseCode = "401",
            description = "Unauthorized - valid access token required"
        )
    })
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        String email = securityContext.getUserPrincipal().getName();
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new WebApplicationException("User not found", Response.Status.NOT_FOUND));

        UserResponse userResponse = new UserResponse(
            user.getEmail(),
            user.getUsername(),
            user.getRoles().stream().map(Enum::name).toList()
        );

        return Response.ok(userResponse).build();
    }

    @POST @Path("/logout")
    @Operation(
        summary = "User logout",
        description = "Clears authentication cookies and invalidates the session"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Logout successful"
        )
    })
    public Response logout(
        @Context HttpServletRequest request,
        @Context HttpServletResponse response
    ) {
        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear authentication cookies
        // Note: Secure flag doesn't matter when deleting cookies
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false); // Allow deletion in development
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0); // Delete cookie
        accessCookie.setAttribute("SameSite", "Lax"); // Lax for development compatibility
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // Allow deletion in development
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // Delete cookie
        refreshCookie.setAttribute("SameSite", "Lax"); // Lax for development compatibility
        response.addCookie(refreshCookie);

        return Response.ok(new SuccessResponse("Logged out successfully")).build();
    }

    // Response DTOs
    public static class UserResponse {
        public String email;
        public String username;
        public java.util.List<String> roles;

        public UserResponse(String email, String username, java.util.List<String> roles) {
            this.email = email;
            this.username = username;
            this.roles = roles;
        }
    }

    public static class LoginSuccessResponse {
        public String message;
        public String returnTo;

        public LoginSuccessResponse(String message, String returnTo) {
            this.message = message;
            this.returnTo = returnTo;
        }
    }

    public static class TwoFactorResponse {
        public String error;
        public String error_description;
        public boolean twoFactorRequired = true;

        public TwoFactorResponse(String error, String description) {
            this.error = error;
            this.error_description = description;
        }
    }

    public static class SuccessResponse {
        public String message;
        public SuccessResponse(String message) { this.message = message; }
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    public static class VerificationPendingResponse {
        public String message;
        public String email;
        public int expiresInMinutes;
        public boolean verificationRequired = true;

        public VerificationPendingResponse(String message, String email, int expiresInMinutes) {
            this.message = message;
            this.email = email;
            this.expiresInMinutes = expiresInMinutes;
        }
    }
}