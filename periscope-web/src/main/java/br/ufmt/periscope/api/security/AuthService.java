package br.ufmt.periscope.api.security;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.security.UserAuthenticator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthService {

    @Inject
    private UserAuthenticator userAuthenticator;

    @Inject
    private JwtService jwtService;

    @Inject
    private TokenRevocationStore revocationStore;

    public AuthResult authenticate(String username, String password) {
        User user = userAuthenticator.authenticate(username, password);
        if (user == null) {
            return null;
        }
        String level = user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        boolean mustChange = Boolean.TRUE.equals(user.getMustChangePassword());
        String token = jwtService.issueToken(user.getUsername(), level, mustChange, user.getTokenVersion());
        return new AuthResult(token, user);
    }

    public JwtService.JwtClaims validate(String token) {
        JwtService.JwtClaims claims = jwtService.parseAndValidate(token);
        if (revocationStore != null) {
            if (claims.jti() != null && revocationStore.isRevoked(claims.jti())) {
                throw new IllegalArgumentException("Token revoked");
            }
            if (revocationStore.isVersionRevoked(claims.subject(), claims.tokenVersion())) {
                throw new IllegalArgumentException("Token revoked");
            }
        }
        return claims;
    }

    public void revoke(JwtService.JwtClaims claims) {
        if (revocationStore != null && claims != null && claims.jti() != null) {
            revocationStore.revoke(claims.jti(), claims.exp());
        }
    }

    public AuthResult refresh(User user, JwtService.JwtClaims currentClaims) {
        revoke(currentClaims);
        String level = user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        boolean mustChange = Boolean.TRUE.equals(user.getMustChangePassword());
        String token = jwtService.issueToken(user.getUsername(), level, mustChange, user.getTokenVersion());
        return new AuthResult(token, user);
    }

    public AuthResult rotateAfterPasswordChange(User user, JwtService.JwtClaims currentClaims) {
        revoke(currentClaims);
        if (revocationStore != null && user != null) {
            revocationStore.revokeAllForUserBeforeVersion(user.getUsername(), user.getTokenVersion());
        }
        String level = user != null && user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        String token = jwtService.issueToken(
                user != null ? user.getUsername() : "",
                level,
                false,
                user != null ? user.getTokenVersion() : 0L);
        return new AuthResult(token, user);
    }

    public long getExpirationSeconds() {
        return jwtService != null ? jwtService.getExpirationSeconds() : 8 * 3600L;
    }

    public record AuthResult(String token, User user) {
    }
}
