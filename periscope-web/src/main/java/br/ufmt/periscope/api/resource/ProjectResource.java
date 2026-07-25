package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.ProjectDTO;
import br.ufmt.periscope.api.dto.ProjectRequest;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.Project;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.repository.ProjectRepository;
import dev.morphia.Datastore;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.Date;
import java.util.List;

@Path("/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed({"ADMIN", "USER"})
public class ProjectResource {

    @Inject
    private Datastore ds;

    @Inject
    private ProjectRepository projectRepository;

    @Inject
    private ApiSecuritySupport securitySupport;

    @GET
    public List<ProjectDTO> list(@Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return projectRepository.getProjectList(user).stream()
                .map(p -> ProjectDTO.from(p, securitySupport.countPatents(p)))
                .toList();
    }

    @GET
    @Path("/{id}")
    public ProjectDTO get(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @POST
    public Response create(ProjectRequest request, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw ApiException.badRequest("title is required");
        }
        Project project = new Project();
        project.setTitle(request.title().trim());
        project.setDescription(request.description());
        project.setIsPublic(request.isPublic() != null ? request.isPublic() : Boolean.FALSE);
        project.setCreatedAt(new Date());
        project.setUpdateAt(new Date());
        project.setOwner(user);
        ds.save(project);
        ProjectDTO dto = ProjectDTO.from(project, 0);
        return Response.created(URI.create("/periscope/rest/projects/" + dto.id()))
                .entity(dto)
                .build();
    }

    @PUT
    @Path("/{id}")
    public ProjectDTO update(@PathParam("id") String id, ProjectRequest request,
                             @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        if (request == null) {
            throw ApiException.badRequest("Body is required");
        }
        if (request.title() != null && !request.title().isBlank()) {
            project.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.isPublic() != null) {
            project.setIsPublic(request.isPublic());
        }
        project.setUpdateAt(new Date());
        ds.save(project);
        return ProjectDTO.from(project, securitySupport.countPatents(project));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id, @Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        Project project = securitySupport.requireProject(id, user);
        if (user.getUserLevel() != br.ufmt.periscope.model.UserLevel.ADMIN
                && (project.getOwner() == null || !project.getOwner().getId().equals(user.getId()))) {
            throw ApiException.forbidden("Only the owner or an admin can delete the project");
        }
        projectRepository.deleteProject(id);
        return Response.noContent().build();
    }
}
