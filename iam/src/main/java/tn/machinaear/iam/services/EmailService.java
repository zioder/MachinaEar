package tn.machinaear.iam.services;

import jakarta.ejb.EJBException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static final String SMTP_HOST_KEY = "smtp.host";
    private static final String SMTP_PORT_KEY = "smtp.port";
    private static final String SMTP_USERNAME_KEY = "smtp.username";
    private static final String SMTP_PASSWORD_KEY = "smtp.password";
    private static final String SMTP_STARTTLS_KEY = "smtp.starttls.enable";

    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;
    private final boolean startTlsEnabled;

    public EmailService() {
        // Use defaults if config is not available (e.g., in tests)
        Config config;
        try {
            config = ConfigProvider.getConfig();
        } catch (Exception e) {
            config = null;
        }
        
        this.smtpHost = getConfigValue(config, SMTP_HOST_KEY, String.class, "localhost");
        this.smtpPort = getConfigValue(config, SMTP_PORT_KEY, Integer.class, 587);
        this.smtpUser = getConfigValue(config, SMTP_USERNAME_KEY, String.class, "");
        this.smtpPassword = getConfigValue(config, SMTP_PASSWORD_KEY, String.class, "");
        this.startTlsEnabled = getConfigValue(config, SMTP_STARTTLS_KEY, Boolean.class, true);
    }

    public void sendEmail(String from, String to, String subject, String content) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        });

        try {
            Message message = createEmailMessage(session, from, to, subject, content);
            Transport.send(message);
            LOGGER.info("Email sent successfully to: " + to);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Failed to send email to: " + to, e);
            throw new EJBException("Failed to send email. Please check the configuration and recipient details.", e);
        }
    }

    private Message createEmailMessage(Session session, String from, String to, String subject, String content) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

    private <T> T getConfigValue(Config config, String propertyName, Class<T> propertyType, T defaultValue) {
        if (config != null) {
            try {
                return config.getOptionalValue(propertyName, propertyType).orElse(defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
