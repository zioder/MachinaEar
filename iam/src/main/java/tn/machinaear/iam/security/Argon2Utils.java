package tn.machinaear.iam.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class Argon2Utils {
    private final Argon2 argon2;
    private final int iterations;
    private final int memory;
    private final int threadNumber;

    public Argon2Utils() {
        // Use defaults if config is not available (e.g., in tests)
        Config config;
        try {
            config = ConfigProvider.getConfig();
        } catch (Exception e) {
            config = null;
        }
        
        int saltLength = getConfigValue(config, "argon2.saltLength", 32);
        int hashLength = getConfigValue(config, "argon2.hashLength", 128);
        this.iterations = getConfigValue(config, "argon2.iterations", 23);
        this.memory = getConfigValue(config, "argon2.memory", 97579);
        this.threadNumber = getConfigValue(config, "argon2.threadNumber", 2);
        
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, saltLength, hashLength);
    }

    private int getConfigValue(Config config, String key, int defaultValue) {
        if (config != null) {
            try {
                return config.getOptionalValue(key, Integer.class).orElse(defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public boolean check(String dbHash, char[] clientHash) {
        try {
            return argon2.verify(dbHash, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }

    public String hash(char[] clientHash) {
        try {
            return argon2.hash(iterations, memory, threadNumber, clientHash);
        } finally {
            argon2.wipeArray(clientHash);
        }
    }
}