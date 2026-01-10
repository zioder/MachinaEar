package MachinaEar.iam.controllers.managers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.json.Json;
import jakarta.json.JsonObject;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private HttpClient httpClient;
    private String apiKey;
    private String fromEmail;
    private String fromName;
    private String appUrl;
    private boolean enabled;

    @PostConstruct
    public void init() {
        this.apiKey = System.getenv("RESEND_API_KEY");
        this.fromEmail = getEnvOrDefault("RESEND_FROM_EMAIL", "noreply@machinaear.com");
        this.fromName = getEnvOrDefault("RESEND_FROM_NAME", "MachinaEar");
        this.appUrl = getEnvOrDefault("APP_URL", "http://localhost:8080");

        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warning("RESEND_API_KEY not set. Email service is DISABLED. Set environment variable to enable.");
            this.enabled = false;
        } else {
            this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            this.enabled = true;
            LOGGER.info("EmailService initialized successfully with Resend");
        }
    }

    /**
     * Send email verification link to user
     * @param toEmail Recipient email address
     * @param verificationToken The verification token
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendVerificationEmail(String toEmail, String verificationToken) {
        if (!enabled) {
            LOGGER.warning("Email service disabled. Verification email not sent to: " + toEmail);
            return false;
        }

        String verificationUrl = appUrl + "/auth/verify-email?token=" + verificationToken;

        String subject = "Verify Your Email - MachinaEar";
        String htmlContent = buildVerificationEmailHtml(verificationUrl);
        String textContent = buildVerificationEmailText(verificationUrl);

        return sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send password reset link to user
     * @param toEmail Recipient email address
     * @param resetToken The password reset token
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        if (!enabled) {
            LOGGER.warning("Email service disabled. Password reset email not sent to: " + toEmail);
            return false;
        }

        String resetUrl = appUrl + "/auth/reset-password?token=" + resetToken;

        String subject = "Reset Your Password - MachinaEar";
        String htmlContent = buildPasswordResetEmailHtml(resetUrl);
        String textContent = buildPasswordResetEmailText(resetUrl);

        return sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Core method to send an email via Resend API
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent, String textContent) {
        try {
            // Build JSON request body for Resend API
            JsonObject emailJson = Json.createObjectBuilder()
                .add("from", fromName + " <" + fromEmail + ">")
                .add("to", Json.createArrayBuilder().add(toEmail))
                .add("subject", subject)
                .add("html", htmlContent)
                .add("text", textContent)
                .build();

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(emailJson.toString()))
                .build();

            // Send request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOGGER.info("Email sent successfully to: " + toEmail + " (Status: " + response.statusCode() + ")");
                return true;
            } else {
                LOGGER.warning("Failed to send email to: " + toEmail +
                             " (Status: " + response.statusCode() + ", Body: " + response.body() + ")");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error sending email to: " + toEmail, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Build HTML email for email verification
     */
    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0;">Welcome to MachinaEar!</h1>
                </div>
                <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2 style="color: #333;">Verify Your Email Address</h2>
                    <p>Thank you for registering with MachinaEar. To complete your registration, please verify your email address by clicking the button below:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">Verify Email Address</a>
                    </div>
                    <p style="color: #666; font-size: 14px;">If the button doesn't work, copy and paste this link into your browser:</p>
                    <p style="color: #667eea; word-break: break-all; font-size: 12px;">%s</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                    <p style="color: #999; font-size: 12px;">This link will expire in 24 hours. If you didn't create an account, you can safely ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(verificationUrl, verificationUrl);
    }

    /**
     * Build plain text email for email verification
     */
    private String buildVerificationEmailText(String verificationUrl) {
        return """
            Welcome to MachinaEar!

            Thank you for registering. To complete your registration, please verify your email address by visiting this link:

            %s

            This link will expire in 24 hours.

            If you didn't create an account, you can safely ignore this email.

            ---
            MachinaEar Team
            """.formatted(verificationUrl);
    }

    /**
     * Build HTML email for password reset
     */
    private String buildPasswordResetEmailHtml(String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0;">Password Reset Request</h1>
                </div>
                <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2 style="color: #333;">Reset Your Password</h2>
                    <p>We received a request to reset your password. Click the button below to create a new password:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;">Reset Password</a>
                    </div>
                    <p style="color: #666; font-size: 14px;">If the button doesn't work, copy and paste this link into your browser:</p>
                    <p style="color: #667eea; word-break: break-all; font-size: 12px;">%s</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 30px 0;">
                    <p style="color: #999; font-size: 12px;">This link will expire in 24 hours. If you didn't request a password reset, you can safely ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(resetUrl, resetUrl);
    }

    /**
     * Build plain text email for password reset
     */
    private String buildPasswordResetEmailText(String resetUrl) {
        return """
            Password Reset Request

            We received a request to reset your password. Visit this link to create a new password:

            %s

            This link will expire in 24 hours.

            If you didn't request a password reset, you can safely ignore this email.

            ---
            MachinaEar Team
            """.formatted(resetUrl);
    }

    /**
     * Utility to get environment variable with default value
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Check if email service is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
