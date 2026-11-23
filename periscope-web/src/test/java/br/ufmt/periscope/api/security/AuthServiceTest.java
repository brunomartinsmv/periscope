package br.ufmt.periscope.api.security;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.security.UserAuthenticator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthenticator userAuthenticator;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationStore revocationStore;

    @InjectMocks
    private AuthService authService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin");
        admin.setUserLevel(UserLevel.ADMIN);
    }

    @Test
    void authenticateReturnsTokenAndUser() {
        when(userAuthenticator.authenticate("admin", "123456")).thenReturn(admin);
        when(jwtService.issueToken("admin", "ADMIN", false, 0L)).thenReturn("jwt-token");

        AuthService.AuthResult result = authService.authenticate("admin", "123456");

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().getUsername()).isEqualTo("admin");
    }

    @Test
    void authenticateReturnsNullOnFailure() {
        when(userAuthenticator.authenticate(anyString(), anyString())).thenReturn(null);
        assertThat(authService.authenticate("admin", "wrong")).isNull();
    }

    @Test
    void validateDelegatesToJwtServiceAndChecksRevocation() {
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 0L, "jti-1");
        when(jwtService.parseAndValidate("tok")).thenReturn(claims);
        when(revocationStore.isRevoked("jti-1")).thenReturn(false);
        when(revocationStore.isVersionRevoked("admin", 0L)).thenReturn(false);
        assertThat(authService.validate("tok")).isEqualTo(claims);
    }

    @Test
    void validateRejectsRevokedToken() {
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 0L, "jti-revoked");
        when(jwtService.parseAndValidate("tok")).thenReturn(claims);
        when(revocationStore.isRevoked("jti-revoked")).thenReturn(true);

        assertThatThrownBy(() -> authService.validate("tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void validateRejectsVersionRevokedToken() {
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 0L, "jti-ok", false, 0L);
        when(jwtService.parseAndValidate("tok")).thenReturn(claims);
        when(revocationStore.isRevoked("jti-ok")).thenReturn(false);
        when(revocationStore.isVersionRevoked("admin", 0L)).thenReturn(true);

        assertThatThrownBy(() -> authService.validate("tok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refreshRevokesOldClaimsAndIssuesNewToken() {
        JwtService.JwtClaims oldClaims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 10L, "jti-old");
        when(jwtService.issueToken("admin", "ADMIN", false, 0L)).thenReturn("new-jwt");

        AuthService.AuthResult refreshed = authService.refresh(admin, oldClaims);

        verify(revocationStore).revoke("jti-old", 9999L);
        assertThat(refreshed.token()).isEqualTo("new-jwt");
    }

    @Test
    void rotateAfterPasswordChangeRevokesJtiAndPriorVersions() {
        admin.setTokenVersion(1L);
        JwtService.JwtClaims oldClaims = new JwtService.JwtClaims("admin", "ADMIN", 9999L, 10L, "jti-pw", true, 0L);
        when(jwtService.issueToken("admin", "ADMIN", false, 1L)).thenReturn("rotated-jwt");

        AuthService.AuthResult rotated = authService.rotateAfterPasswordChange(admin, oldClaims);

        verify(revocationStore).revoke("jti-pw", 9999L);
        verify(revocationStore).revokeAllForUserBeforeVersion("admin", 1L);
        assertThat(rotated.token()).isEqualTo("rotated-jwt");
    }
}
