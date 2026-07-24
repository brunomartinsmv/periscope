package br.ufmt.periscope.api.resource;

import br.ufmt.periscope.api.ApiException;
import br.ufmt.periscope.api.dto.LoginRequest;
import br.ufmt.periscope.api.dto.LoginResponse;
import br.ufmt.periscope.api.dto.UserDTO;
import br.ufmt.periscope.api.security.AuthService;
import br.ufmt.periscope.api.security.ApiSecuritySupport;
import br.ufmt.periscope.model.User;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@Tag(name = "Auth")
public class AuthResource {

    @Inject
    private AuthService authService;

    @Inject
    private ApiSecuritySupport securitySupport;

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login", description = "Autentica e devolve JWT + usuário")
    @APIResponse(responseCode = "200", description = "Token emitido")
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public Response login(LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            throw ApiException.badRequest("username and password are required");
        }
        AuthService.AuthResult result = authService.authenticate(request.username(), request.password());
        if (result == null) {
            throw ApiException.unauthorized("Invalid username or password");
        }
        return Response.ok(new LoginResponse(result.token(), UserDTO.from(result.user()))).build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Usuário atual")
    @SecurityRequirement(name = "bearerAuth")
    public UserDTO me(@Context SecurityContext securityContext) {
        User user = securitySupport.requireUser(securityContext);
        return UserDTO.from(user);
    }

    /**
     * Stateless logout: client discards the JWT. Always returns 204.
     */
    @POST
    @Path("/logout")
    @RolesAllowed({"ADMIN", "USER"})
    @Operation(summary = "Logout stateless", description = "Cliente deve descartar o JWT")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponse(responseCode = "204", description = "Sem conteúdo")
    public Response logout() {
        return Response.noContent().build();
    }
}
