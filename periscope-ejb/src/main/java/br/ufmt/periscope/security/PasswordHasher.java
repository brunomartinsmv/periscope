package br.ufmt.periscope.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 password hashing with transparent legacy plain-text support.
 * Stored format: {@code pbkdf2$&lt;iterations&gt;$&lt;saltB64&gt;$&lt;hashB64&gt;}.
 */
public final class PasswordHasher {

    public static final String PREFIX = "pbkdf2$";
    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return hash(plainPassword, DEFAULT_ITERATIONS);
    }

    public static String hash(String plainPassword, int iterations) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        if (iterations < 120_000) {
            throw new IllegalArgumentException("iterations must be >= 120000");
        }
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, iterations);
        return PREFIX + iterations + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies {@code plainPassword} against a stored value that may be a PBKDF2
     * hash or legacy plain text.
     */
    public static boolean verify(String plainPassword, String stored) {
        if (plainPassword == null || stored == null) {
            return false;
        }
        if (isHashed(stored)) {
            return verifyHashed(plainPassword, stored);
        }
        return constantTimeEquals(
                plainPassword.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static boolean needsRehash(String stored) {
        return !isHashed(stored);
    }

    private static boolean verifyHashed(String plainPassword, String stored) {
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(plainPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 unavailable", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            // still compare to avoid trivial timing leak on length
            return MessageDigest.isEqual(a, a) && false;
        }
        return MessageDigest.isEqual(a, b);
    }
}
