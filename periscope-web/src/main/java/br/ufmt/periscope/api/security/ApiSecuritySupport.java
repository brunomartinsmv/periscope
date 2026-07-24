package br.ufmt.periscope.api.security;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.repository.ProjectRepository;
import dev.morphia.Datastore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import org.bson.types.ObjectId;

import static dev.morphia.query.filters.Filters.eq;

@ApplicationScoped
public class ApiSecuritySupport {

    @Inject
    private Datastore ds;

    @Inject
    private ProjectRepository projectRepository;

    public User requireUser(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw ApiException.unauthorized("Not authenticated");
        }
        String username = securityContext.getUserPrincipal().getName();
        User user = ds.find(User.class).filter(eq("username", username)).first();
        if (user == null) {
            throw ApiException.unauthorized("User not found");
        }
        return user;
    }

    public Project requireProject(String projectId, User user) {
        if (projectId == null || !ObjectId.isValid(projectId)) {
            throw ApiException.badRequest("Invalid project id");
        }
        Project project = ds.find(Project.class)
                .filter(eq("_id", new ObjectId(projectId)))
                .first();
        if (project == null) {
            throw ApiException.notFound("Project not found");
        }
        if (!canAccess(project, user)) {
            throw ApiException.forbidden("No access to this project");
        }
        return project;
    }

    public boolean canAccess(Project project, User user) {
        if (user.getUserLevel() == UserLevel.ADMIN) {
            return true;
        }
        return projectRepository.getProjectList(user).stream()
                .anyMatch(p -> p.getId() != null && p.getId().equals(project.getId()));
    }

    public long countPatents(Project project) {
        return ds.find(br.ufmt.periscope.model.Patent.class)
                .filter(eq("project", project))
                .count();
    }
}
