package br.ufmt.periscope.managedbean;

import java.io.Serializable;

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

import static dev.morphia.query.filters.Filters.eq;

import dev.morphia.Datastore;

@Named
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 440310707447932765L;

    @Inject
    private Datastore ds;
    @Inject
    private LoginController loginController;
    private User loggedUser;

    public String login() {
        User u = ds.find(User.class)
                .filter(
                        eq("username", loginController.getLogin()),
                        eq("password", loginController.getPassword()))
                .first();

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
