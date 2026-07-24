package br.ufmt.periscope.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.setSecretForTests("unit-test-secret-key-32bytes!!".getBytes(StandardCharsets.UTF_8));
        jwtService.setExpirationSecondsForTests(3600);
    }

    @Test
    void issuesAndValidatesToken() {
        String token = jwtService.issueToken("admin", "ADMIN");
        assertThat(token.split("\\.")).hasSize(3);

        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
        assertThat(claims.subject()).isEqualTo("admin");
        assertThat(claims.userLevel()).isEqualTo("ADMIN");
        assertThat(claims.exp()).isGreaterThan(claims.iat());
    }

    @Test
    void rejectsTamperedSignature() {
        String token = jwtService.issueToken("admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 2) + "ab";
        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void rejectsExpiredToken() {
        jwtService.setExpirationSecondsForTests(-10);
        String token = jwtService.issueToken("admin", "USER");
        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseAndValidate("not-a-jwt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.parseAndValidate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
