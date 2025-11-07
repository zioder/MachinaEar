package MachinaEar.iam.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public final class Argon2Utility {
    private Argon2Utility() {}

    public static String hash(char[] password) {
        Argon2 argon2 = Argon2Factory.create(); // Argon2id par défaut
        try {
            return argon2.hash(3, 1 << 15, 2, password); // itérations, mémoire (32MB), parallélisme
        } finally {
            argon2.wipeArray(password);
        }
    }

    public static boolean verify(String hash, char[] password) {
        Argon2 argon2 = Argon2Factory.create();
        try {
            return argon2.verify(hash, password);
        } finally {
            argon2.wipeArray(password);
        }
    }
}
