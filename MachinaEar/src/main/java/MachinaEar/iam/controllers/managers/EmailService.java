package MachinaEar.iam.controllers.managers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    /**
     * NOTE: Avoid String.format / String::formatted for email templates.
     * A stray '%' in template content (common in HTML/CSS) can cause runtime formatter exceptions.
     */
    private static final String VERIFY_TEXT_TEMPLATE = """
            Welcome to MachinaEar!

            Thank you for registering. To complete your registration, please verify your email address by visiting this link:

            {{url}}

            This link will expire in 24 hours.

            If you didn't create an account, you can safely ignore this email.

            ---
            MachinaEar Team
            """;

    private static final String RESET_TEXT_TEMPLATE = """
            Password Reset Request

            We received a request to reset your password. Visit this link to create a new password:

            {{url}}

            This link will expire in 24 hours.

            If you didn't request a password reset, you can safely ignore this email.

            ---
            MachinaEar Team
            """;

    private static final String VERIFY_CODE_TEXT_TEMPLATE = """
            Welcome to MachinaEar!

            Your verification code is: {{code}}

            Enter this code on the registration page to complete your account setup.

            This code will expire in 15 minutes.

            If you didn't create an account, you can safely ignore this email.

            ---
            MachinaEar Team
            """;

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String fromEmail;
    private String fromName;
    private String appUrl;
    private String iamUrl;  // URL for IAM frontend (login page)
    private boolean auth;
    private boolean starttls;
    private boolean enabled;

    @PostConstruct
    public void init() {
        this.smtpHost = getEnvOrDefault("SMTP_HOST", null);
        this.smtpPort = Integer.parseInt(getEnvOrDefault("SMTP_PORT", "587"));
        this.smtpUser = getEnvOrDefault("SMTP_USER", null);
        this.smtpPassword = getEnvOrDefault("SMTP_PASSWORD", null);
        this.fromEmail = getEnvOrDefault("SMTP_FROM_EMAIL", "support@machinaear.me");
        this.fromName = getEnvOrDefault("SMTP_FROM_NAME", "MachinaEar Support");
        this.appUrl = getEnvOrDefault("APP_URL", "https://machinaear.me");
        this.iamUrl = getEnvOrDefault("IAM_URL", "https://iam.machinaear.me/iam-0.1.0/login.html");
        this.auth = Boolean.parseBoolean(getEnvOrDefault("SMTP_AUTH", "true"));
        this.starttls = Boolean.parseBoolean(getEnvOrDefault("SMTP_STARTTLS", "true"));

        if (smtpHost == null || smtpHost.isBlank()) {
            LOGGER.warning("SMTP_HOST not set. Email service is DISABLED. Set environment variable to enable.");
            this.enabled = false;
        } else {
            this.enabled = true;
            LOGGER.info("EmailService initialized successfully with SMTP host: " + smtpHost);
        }
    }

    /**
     * Send email verification link to user (legacy - for existing tokens)
     * @param toEmail Recipient email address
     * @param verificationToken The verification token
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendVerificationEmail(String toEmail, String verificationToken) {
        if (!enabled) {
            LOGGER.warning("Email service disabled. Verification email not sent to: " + toEmail);
            return false;
        }

        String verificationUrl = buildAppLink("/auth/verify-email", "token", verificationToken);

        String subject = "Verify Your Email - MachinaEar";
        String htmlContent = buildVerificationEmailHtml(verificationUrl);
        String textContent = buildVerificationEmailText(verificationUrl);

        return sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Send verification CODE email to user (new flow - 6-digit code)
     * @param toEmail Recipient email address
     * @param verificationCode The 6-digit verification code
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendVerificationCodeEmail(String toEmail, String verificationCode) {
        if (!enabled) {
            LOGGER.warning("Email service disabled. Verification code email not sent to: " + toEmail);
            return false;
        }

        String subject = "Your Verification Code - MachinaEar";
        String htmlContent = buildVerificationCodeEmailHtml(verificationCode);
        String textContent = VERIFY_CODE_TEXT_TEMPLATE.replace("{{code}}", verificationCode);

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

        // Build URL to the IAM login page with reset-password mode
        String resetUrl = buildIamLink("reset-password", resetToken);

        String subject = "Reset Your Password - MachinaEar";
        String htmlContent = buildPasswordResetEmailHtml(resetUrl);
        String textContent = buildPasswordResetEmailText(resetUrl);

        return sendEmail(toEmail, subject, htmlContent, textContent);
    }

    /**
     * Core method to send an email via SMTP
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent, String textContent) {
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(auth));
        
        if (smtpPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        }
        
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(smtpUser, smtpPassword);
            }
        });

        try {
            jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(fromEmail, fromName));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Create a multi-part message for HTML and Text
            jakarta.mail.internet.MimeMultipart multipart = new jakarta.mail.internet.MimeMultipart("alternative");

            // Text part
            jakarta.mail.internet.MimeBodyPart textPart = new jakarta.mail.internet.MimeBodyPart();
            textPart.setText(textContent, "utf-8");
            multipart.addBodyPart(textPart);

            // HTML part
            jakarta.mail.internet.MimeBodyPart htmlPart = new jakarta.mail.internet.MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            message.setContent(multipart);

            jakarta.mail.Transport.send(message);

            LOGGER.info("Email sent successfully via SMTP to: " + toEmail);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sending SMTP email to: " + toEmail, e);
            return false;
        }
    }

    /**
     * Build HTML email for email verification (link-based)
     */
    private String buildVerificationEmailHtml(String verificationUrl) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<div style=\"background: #667eea; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;\">" +
            "<h1 style=\"color: white; margin: 0;\">Welcome to MachinaEar!</h1></div>" +
            "<div style=\"background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;\">" +
            "<h2 style=\"color: #333;\">Verify Your Email Address</h2>" +
            "<p>Thank you for registering with MachinaEar. To complete your registration, please verify your email address by clicking the button below:</p>" +
            "<div style=\"text-align: center; margin: 30px 0;\">" +
            "<a href=\"" + verificationUrl + "\" style=\"background: #667eea; color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;\">Verify Email Address</a></div>" +
            "<p style=\"color: #666; font-size: 14px;\">If the button doesn't work, copy and paste this link into your browser:</p>" +
            "<p style=\"color: #667eea; word-break: break-all; font-size: 12px;\">" + verificationUrl + "</p>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">" +
            "<p style=\"color: #999; font-size: 12px;\">This link will expire in 24 hours. If you didn't create an account, you can safely ignore this email.</p>" +
            "</div></body></html>";
    }

    /**
     * Build HTML email for verification CODE (6-digit code)
     */
    private String buildVerificationCodeEmailHtml(String code) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<div style=\"background: #667eea; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;\">" +
            "<h1 style=\"color: white; margin: 0;\">Welcome to MachinaEar!</h1></div>" +
            "<div style=\"background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;\">" +
            "<h2 style=\"color: #333;\">Your Verification Code</h2>" +
            "<p>Thank you for registering with MachinaEar. Enter the following code to complete your registration:</p>" +
            "<div style=\"text-align: center; margin: 30px 0;\">" +
            "<div style=\"background: #667eea; color: white; padding: 20px 40px; font-size: 32px; font-weight: bold; letter-spacing: 8px; border-radius: 10px; display: inline-block;\">" + code + "</div></div>" +
            "<p style=\"color: #666; font-size: 14px; text-align: center;\">This code will expire in <strong>15 minutes</strong>.</p>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">" +
            "<p style=\"color: #999; font-size: 12px;\">If you didn't create an account, you can safely ignore this email.</p>" +
            "</div></body></html>";
    }

    /**
     * Build plain text email for email verification
     */
    private String buildVerificationEmailText(String verificationUrl) {
        return VERIFY_TEXT_TEMPLATE.replace("{{url}}", verificationUrl);
    }

    /**
     * Build HTML email for password reset
     */
    private String buildPasswordResetEmailHtml(String resetUrl) {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
            "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;\">" +
            "<div style=\"background: #667eea; padding: 30px; text-align: center; border-radius: 10px 10px 0 0;\">" +
            "<h1 style=\"color: white; margin: 0;\">Password Reset Request</h1></div>" +
            "<div style=\"background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;\">" +
            "<h2 style=\"color: #333;\">Reset Your Password</h2>" +
            "<p>We received a request to reset your password. Click the button below to create a new password:</p>" +
            "<div style=\"text-align: center; margin: 30px 0;\">" +
            "<a href=\"" + resetUrl + "\" style=\"background: #667eea; color: white; padding: 14px 30px; text-decoration: none; border-radius: 5px; display: inline-block; font-weight: bold;\">Reset Password</a></div>" +
            "<p style=\"color: #666; font-size: 14px;\">If the button doesn't work, copy and paste this link into your browser:</p>" +
            "<p style=\"color: #667eea; word-break: break-all; font-size: 12px;\">" + resetUrl + "</p>" +
            "<hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">" +
            "<p style=\"color: #999; font-size: 12px;\">This link will expire in 24 hours. If you didn't request a password reset, you can safely ignore this email.</p>" +
            "</div></body></html>";
    }

    /**
     * Build plain text email for password reset
     */
    private String buildPasswordResetEmailText(String resetUrl) {
        return RESET_TEXT_TEMPLATE.replace("{{url}}", resetUrl);
    }

    private String buildAppLink(String path, String paramName, String paramValue) {
        String base = (appUrl == null) ? "" : appUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String encoded = URLEncoder.encode(paramValue == null ? "" : paramValue, StandardCharsets.UTF_8);
        return base + path + "?" + paramName + "=" + encoded;
    }

    /**
     * Build a link to the IAM login page with specific mode and token.
     * Example: https://iam.machinaear.me/iam-0.1.0/login.html?mode=reset-password&token=...
     */
    private String buildIamLink(String mode, String token) {
        String base = (iamUrl == null) ? "" : iamUrl.trim();
        String encoded = URLEncoder.encode(token == null ? "" : token, StandardCharsets.UTF_8);
        
        // Check if base already has query params
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "mode=" + mode + "&token=" + encoded;
    }

    /**
     * Utility to get environment variable with default value
     * Checks both Environment Variables and System Properties
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Check if email service is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
