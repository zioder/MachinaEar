package MachinaEar.iam.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Manages TOTP (Time-based One-Time Password) for two-factor authentication.
 * Uses Google Authenticator compatible algorithm.
 */
@ApplicationScoped
public class TotpManager {

    private final GoogleAuthenticator gAuth;
    private final SecureRandom secureRandom;

    public TotpManager() {
        this.gAuth = new GoogleAuthenticator();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a new TOTP secret key for a user.
     *
     * @return Base32 encoded secret key
     */
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Verifies a TOTP code against the user's secret.
     *
     * @param secret the user's TOTP secret (Base32 encoded)
     * @param code the 6-digit code to verify
     * @return true if the code is valid
     */
    public boolean verifyCode(String secret, int code) {
        try {
            return gAuth.authorize(secret, code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a QR code URL that can be scanned by authenticator apps.
     *
     * @param email the user's email
     * @param secret the TOTP secret
     * @param issuer the application name (e.g., "MachinaEar")
     * @return URL for QR code
     */
    public String generateQrCodeUrl(String email, String secret, String issuer) {
        try {

            String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8.toString());
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());

            return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    encodedIssuer, encodedEmail, secret, encodedIssuer);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to encode URL", e);
        }
    }

    /**
     * Generates a QR code image as Base64 encoded PNG.
     *
     * @param email the user's email
     * @param secret the TOTP secret
     * @param issuer the application name
     * @return Base64 encoded PNG image
     * @throws WriterException if QR code generation fails
     * @throws IOException if image encoding fails
     */
    public String generateQrCodeImage(String email, String secret, String issuer)
            throws WriterException, IOException {
        String url = generateQrCodeUrl(email, secret, issuer);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();

        return Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * Generates a set of recovery codes for backup access. Each code is 10
     * characters (alphanumeric).
     *
     * @param count number of recovery codes to generate (typically 10)
     * @return list of recovery codes
     */
    public List<String> generateRecoveryCodes(int count) {
        List<String> codes = new ArrayList<>();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        for (int i = 0; i < count; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 10; j++) {
                code.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
            codes.add(code.toString());
        }

        return codes;
    }

    /**
     * Hashes a recovery code for storage. Uses Argon2 to securely hash recovery
     * codes.
     *
     * @param code the recovery code to hash
     * @return hashed recovery code
     */
    public String hashRecoveryCode(String code) {
        return Argon2Utility.hash(code.toCharArray());
    }

    /**
     * Verifies a recovery code against its hash.
     *
     * @param hash the stored hash
     * @param code the recovery code to verify
     * @return true if the code matches
     */
    public boolean verifyRecoveryCode(String hash, String code) {
        return Argon2Utility.verify(hash, code.toCharArray());
    }

    /**
     * Result of 2FA setup containing secret and QR code.
     */
    public static class SetupResult {

        private final String secret;
        private final String qrCodeUrl;
        private final String qrCodeImage;
        private final List<String> recoveryCodes;

        public SetupResult(String secret, String qrCodeUrl, String qrCodeImage,
                List<String> recoveryCodes) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
            this.qrCodeImage = qrCodeImage;
            this.recoveryCodes = recoveryCodes;
        }

        public String getSecret() {
            return secret;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }

        public String getQrCodeImage() {
            return qrCodeImage;
        }

        public List<String> getRecoveryCodes() {
            return recoveryCodes;
        }
    }
}
