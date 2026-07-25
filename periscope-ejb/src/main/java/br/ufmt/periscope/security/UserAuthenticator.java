package br.ufmt.periscope.security;

import br.ufmt.periscope.model.User;
import dev.morphia.Datastore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static dev.morphia.query.filters.Filters.eq;

/**
 * Shared username/password authentication with transparent PBKDF2 migration.
 */
@ApplicationScoped
public class UserAuthenticator {

    @Inject
    private Datastore ds;

    /**
     * @return authenticated user, or {@code null} if credentials are invalid
     */
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        User user = ds.find(User.class)
                .filter(eq("username", username.trim()))
                .first();
        if (user == null || !PasswordHasher.verify(password, user.getPassword())) {
            return null;
        }
        if (PasswordHasher.needsRehash(user.getPassword())) {
            user.setPassword(PasswordHasher.hash(password));
            ds.save(user);
        }
        return user;
    }
}
