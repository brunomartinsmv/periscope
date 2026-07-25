package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.UserDTO;
import br.ufmt.periscope.api.dto.UserRequest;
import br.ufmt.periscope.model.User;
import br.ufmt.periscope.model.UserLevel;
import br.ufmt.periscope.security.PasswordHasher;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static dev.morphia.query.filters.Filters.eq;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RolesAllowed("ADMIN")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserResource {

    @Inject
    private Datastore ds;

    @GET
    @Operation(summary = "Listar usuários")
    public List<UserDTO> list() {
        return ds.find(User.class).iterator().toList().stream()
                .map(UserDTO::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obter usuário")
    public UserDTO get(@PathParam("id") String id) {
        return UserDTO.from(find(id));
    }

    @POST
    @Operation(summary = "Criar usuário")
    public Response create(UserRequest request) {
        validateCreate(request);
        User existing = ds.find(User.class).filter(eq("username", request.username().trim())).first();
        if (existing != null) {
            throw ApiException.badRequest("username already exists");
        }
        User user = new User();
        apply(user, request, true);
        ds.save(user);
        return Response.created(URI.create("/periscope/rest/users/" + user.getId()))
                .entity(UserDTO.from(user))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Atualizar usuário")
    public UserDTO update(@PathParam("id") String id, UserRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Body is required");
        }
        User user = find(id);
        if (request.username() != null && !request.username().isBlank()) {
            User other = ds.find(User.class).filter(eq("username", request.username().trim())).first();
            if (other != null && !other.getId().equals(user.getId())) {
                throw ApiException.badRequest("username already exists");
            }
            user.setUsername(request.username().trim());
        }
        apply(user, request, false);
        ds.save(user);
        return UserDTO.from(user);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir usuário")
    public Response delete(@PathParam("id") String id) {
        User user = find(id);
        ds.delete(user);
        return Response.noContent().build();
    }

    private User find(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw ApiException.badRequest("Invalid user id");
        }
        User user = ds.find(User.class).filter(eq("_id", new ObjectId(id))).first();
        if (user == null) {
            throw ApiException.notFound("User not found");
        }
        return user;
    }

    private static void validateCreate(UserRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            throw ApiException.badRequest("username is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw ApiException.badRequest("password is required");
        }
    }

    private static void apply(User user, UserRequest request, boolean creating) {
        if (request.firstname() != null) {
            user.setFirstname(request.firstname());
        }
        if (request.lastname() != null) {
            user.setLastname(request.lastname());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.userLevel() != null && !request.userLevel().isBlank()) {
            try {
                user.setUserLevel(UserLevel.valueOf(request.userLevel().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("userLevel must be ADMIN or USER");
            }
        } else if (creating) {
            user.setUserLevel(UserLevel.USER);
        }
        if (creating) {
            user.setUsername(request.username().trim());
            user.setPassword(PasswordHasher.hash(request.password()));
        } else if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(PasswordHasher.hash(request.password()));
        }
    }
}
