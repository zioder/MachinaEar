package MachinaEar.iam.controllers.managers;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String fromEmail;
    private String fromName;
    private String appUrl;
    private boolean auth;
    private boolean starttls;
    private boolean enabled;

    @PostConstruct
    public void init() {
        this.smtpHost = System.getenv("SMTP_HOST");
        this.smtpPort = Integer.parseInt(getEnvOrDefault("SMTP_PORT", "587"));
        this.smtpUser = System.getenv("SMTP_USER");
        this.smtpPassword = System.getenv("SMTP_PASSWORD");
        this.fromEmail = getEnvOrDefault("SMTP_FROM_EMAIL", "support@machinaear.me");
        this.fromName = getEnvOrDefault("SMTP_FROM_NAME", "MachinaEar Support");
        this.appUrl = getEnvOrDefault("APP_URL", "https://machinaear.me");
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
