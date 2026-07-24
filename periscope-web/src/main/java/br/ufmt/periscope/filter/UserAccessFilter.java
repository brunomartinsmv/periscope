package br.ufmt.periscope.filter;

import java.io.IOException;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import br.ufmt.periscope.model.User;
import br.ufmt.periscope.qualifier.LoggedUser;

@WebFilter(urlPatterns = {"/pages/*", "*.jsf"})
public class UserAccessFilter implements Filter {

    private @Inject
    @LoggedUser
    Instance<User> currentUser;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String pageRequested = req.getRequestURI();
        String prefix = req.getContextPath();

        if (pageRequested.endsWith("login.jsf")
                || pageRequested.endsWith("js.jsf")
                || pageRequested.endsWith("css.jsf")) {
            chain.doFilter(request, response);
            return;
        }
        if (session == null) {
            resp.sendRedirect(prefix + "/login.jsf");
            return;
        }
        if (currentUser.get() == null) {
            resp.sendRedirect(prefix + "/login.jsf");
            return;
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {

    }

    @Override
    public void destroy() {

    }

}
