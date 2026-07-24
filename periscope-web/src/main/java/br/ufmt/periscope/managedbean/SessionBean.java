package br.ufmt.periscope.managedbean;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import br.ufmt.periscope.controller.LoginController;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.qualifier.LoggedUser;

import com.github.jmkgreen.morphia.Datastore;
import com.github.jmkgreen.morphia.mapping.lazy.DatastoreHolder;
import com.github.jmkgreen.morphia.query.Query;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 440310707447932765L;
    private Datastore ds;
    private @Inject
    LoginController loginController;
    private User loggedUser;

    @PostConstruct
    public void init() {
        ds = DatastoreHolder.getInstance().get();
    }

    public String login() {
        Query<User> query = ds.find(User.class)
                .field("username").equal(loginController.getLogin())
                .field("password").equal(loginController.getPassword());

        User u = query.get();

        if (u != null) {
            loggedUser = u;
            Flash flash = FacesContext.getCurrentInstance().
                    getExternalContext().getFlash();
            flash.put("success", "Bem vindo ao Periscope");
            flash.keep("success");
            return "login";
        } else {
            FacesMessage msg = new FacesMessage("Usuário/Senha inválidos.", "Erro");
            FacesContext.getCurrentInstance().addMessage(null, msg);
            return null;
        }
    }

    public String logout() {
        loggedUser = null;
        return "logout";
    }

    public boolean isLoggedIn() {
        return loggedUser != null;
    }

    @Named
    public boolean isAdmin() {
        return loggedUser.getUserLevel() == UserLevel.ADMIN;
    }

    @Named
    @Produces
    @LoggedUser
    public User getCurrentUser() {
        return loggedUser;
    }
}
