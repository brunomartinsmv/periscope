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

    public AuthResult authenticate(String username, String password) {
        User user = userAuthenticator.authenticate(username, password);
        if (user == null) {
            return null;
        }
        String level = user.getUserLevel() != null ? user.getUserLevel().name() : "USER";
        String token = jwtService.issueToken(user.getUsername(), level);
        return new AuthResult(token, user);
    }

    public JwtService.JwtClaims validate(String token) {
        return jwtService.parseAndValidate(token);
    }

    public record AuthResult(String token, User user) {
    }
}
