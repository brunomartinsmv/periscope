package br.ufmt.periscope.api.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Minimal HS256 JWT without external JWT libraries.
 * Payload JSON is built/parsed with plain strings + {@link javax.crypto.Mac}.
 */
@ApplicationScoped
public class JwtService {

    private static final Logger LOG = Logger.getLogger(JwtService.class.getName());
    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DEC = Base64.getUrlDecoder();

    private byte[] secret;
    private long expirationSeconds = 8 * 3600L;

    @PostConstruct
    void init() {
        String envSecret = System.getenv("PERISCOPE_JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            secret = envSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            secret = HexFormat.of().formatHex(generated).getBytes(StandardCharsets.UTF_8);
            LOG.warning("PERISCOPE_JWT_SECRET not set; using ephemeral in-memory secret "
                    + "(tokens will be invalid after restart)");
        }
        String expHours = System.getenv("PERISCOPE_JWT_EXPIRATION_HOURS");
        if (expHours != null && !expHours.isBlank()) {
            try {
                expirationSeconds = Long.parseLong(expHours.trim()) * 3600L;
            } catch (NumberFormatException ignored) {
                LOG.warning("Invalid PERISCOPE_JWT_EXPIRATION_HOURS; using default 8h");
            }
        }
    }

    public String issueToken(String username, String userLevel) {
        long iat = Instant.now().getEpochSecond();
        long exp = iat + expirationSeconds;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + escape(username) + "\","
                + "\"userLevel\":\"" + escape(userLevel) + "\","
                + "\"iat\":" + iat + ","
                + "\"exp\":" + exp + "}";
        String header = B64URL.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = B64URL.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = B64URL.encodeToString(hmac(signingInput));
        return signingInput + "." + signature;
    }

    public JwtClaims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed token");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput);
        byte[] actual;
        try {
            actual = B64URL_DEC.decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid token signature encoding");
        }
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("Invalid token signature");
        }
        String payloadJson = new String(B64URL_DEC.decode(parts[1]), StandardCharsets.UTF_8);
        long exp = readLong(payloadJson, "exp");
        if (Instant.now().getEpochSecond() >= exp) {
            throw new IllegalArgumentException("Token expired");
        }
        String sub = readString(payloadJson, "sub");
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("Token missing subject");
        }
        String userLevel = readString(payloadJson, "userLevel");
        if (userLevel == null || userLevel.isBlank()) {
            userLevel = "USER";
        }
        long iat = readLong(payloadJson, "iat");
        return new JwtClaims(sub, userLevel, exp, iat);
    }

    void setSecretForTests(byte[] secretBytes) {
        this.secret = secretBytes;
    }

    void setExpirationSecondsForTests(long seconds) {
        this.expirationSeconds = seconds;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readString(String json, String claim) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(claim) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static long readLong(String json, String claim) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(claim) + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Token missing claim " + claim);
        }
        return Long.parseLong(m.group(1));
    }

    public record JwtClaims(String subject, String userLevel, long exp, long iat) {
    }
}
