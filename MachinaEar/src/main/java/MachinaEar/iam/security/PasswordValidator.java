package MachinaEar.iam.security;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates password strength with entropy checking and complexity requirements.
 * Ensures high-entropy passwords that are resistant to dictionary and brute-force attacks.
 */
public class PasswordValidator {

    private static final int MIN_LENGTH = 12;
    private static final double MIN_ENTROPY_THRESHOLD = 35.0; // Minimum acceptable entropy

    /**
     * Validates password strength and returns validation result.
     *
     * @param password the password to validate
     * @return ValidationResult with success status and error messages
     */
    public static ValidationResult validate(char[] password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.length == 0) {
            errors.add("Password is required");
            return new ValidationResult(false, errors);
        }

        // Check minimum length
        if (password.length < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        // Check for character variety
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        int varietyCount = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) +
                          (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);

        if (varietyCount < 3) {
            errors.add("Password must contain at least 3 of the following: lowercase letters, " +
                      "uppercase letters, numbers, special characters");
        }

        // Check for common patterns (only if basic requirements are met)
        if (errors.isEmpty()) {
            String passwordStr = new String(password);
            if (containsCommonPatterns(passwordStr)) {
                errors.add("Password contains common patterns or sequences");
            }
        }

        // Calculate entropy for informational purposes (but don't enforce strict threshold)
        // For a 12-char password with good variety, we expect reasonable entropy
        double entropy = calculateEntropy(password);

        // Only fail if entropy is extremely low which indicates very poor password
        // Examples: "aaaaaaaaaaaa" (0 bits), "123456789012" (~35 bits), "Password1234" (~41 bits)
        if (entropy < MIN_ENTROPY_THRESHOLD && errors.isEmpty()) {
            errors.add(String.format("Password is too weak (entropy: %.1f bits). " +
                    "Use a better mix of different characters",
                    entropy));
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Calculates the Shannon entropy of a password in bits.
     * Higher entropy means stronger password.
     *
     * @param password the password to analyze
     * @return entropy in bits
     */
    private static double calculateEntropy(char[] password) {
        if (password == null || password.length == 0) return 0.0;

        // Count character frequencies
        int[] frequencies = new int[256];
        for (char c : password) {
            if (c < 256) frequencies[c]++;
        }

        // Calculate Shannon entropy
        double entropy = 0.0;
        int length = password.length;

        for (int freq : frequencies) {
            if (freq > 0) {
                double probability = (double) freq / length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }

        // Total entropy = entropy per character * password length
        return entropy * length;
    }

    /**
     * Checks for common weak patterns in passwords.
     *
     * @param password the password string
     * @return true if common patterns are found
     */
    private static boolean containsCommonPatterns(String password) {
        String lower = password.toLowerCase();

        // Check for sequential characters
        String[] sequences = {"abc", "bcd", "cde", "def", "efg", "fgh", "ghi", "hij",
                            "ijk", "jkl", "klm", "lmn", "mno", "nop", "opq", "pqr",
                            "qrs", "rst", "stu", "tuv", "uvw", "vwx", "wxy", "xyz",
                            "012", "123", "234", "345", "456", "567", "678", "789"};

        for (String seq : sequences) {
            if (lower.contains(seq)) return true;
        }

        // Check for common weak words
        String[] commonWords = {"password", "admin", "user", "login", "welcome",
                               "qwerty", "asdf", "zxcv", "letmein", "monkey"};

        for (String word : commonWords) {
            if (lower.contains(word)) return true;
        }

        // Check for repeated characters (more than 2 in a row)
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Result of password validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}
