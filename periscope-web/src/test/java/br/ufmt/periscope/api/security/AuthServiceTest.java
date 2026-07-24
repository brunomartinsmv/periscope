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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAuthenticator userAuthenticator;

    @Mock
    private JwtService jwtService;

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
        when(jwtService.issueToken("admin", "ADMIN")).thenReturn("jwt-token");

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
    void validateDelegatesToJwtService() {
        JwtService.JwtClaims claims = new JwtService.JwtClaims("admin", "ADMIN", 1L, 0L);
        when(jwtService.parseAndValidate("tok")).thenReturn(claims);
        assertThat(authService.validate("tok")).isEqualTo(claims);
    }
}
