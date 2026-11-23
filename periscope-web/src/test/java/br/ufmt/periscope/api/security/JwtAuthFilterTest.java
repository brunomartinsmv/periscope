package br.ufmt.periscope.api.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private JwtAuthFilter filter;

    private JwtService.JwtClaims forcedChangeClaims;
    private JwtService.JwtClaims normalClaims;

    @BeforeEach
    void setUp() {
        forcedChangeClaims = new JwtService.JwtClaims("admin", "ADMIN", 999999L, 1L, "jti-1", true, 0L);
        normalClaims = new JwtService.JwtClaims("admin", "ADMIN", 999999L, 1L, "jti-2", false, 1L);
    }

    @Test
    void blocksApiRoutesWhenMustChangePasswordIsTrue() throws IOException {
        for (String[] route : new String[][]{
                {"GET", "/projects"},
                {"POST", "/projects/123/patents"},
                {"GET", "/users"},
                {"POST", "/auth/refresh"}
        }) {
            ContainerRequestContext ctx = mockContext(route[0], route[1], "tok-forced");
            when(authService.validate("tok-forced")).thenReturn(forcedChangeClaims);

            filter.filter(ctx);

            ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
            verify(ctx).abortWith(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(403);
        }
    }

    @Test
    void allowsMeChangePasswordAndLogoutWhenMustChangePasswordIsTrue() throws IOException {
        for (String[] route : new String[][]{
                {"GET", "/auth/me"},
                {"POST", "/auth/change-password"},
                {"POST", "/auth/logout"}
        }) {
            ContainerRequestContext ctx = mockContext(route[0], route[1], "tok-forced");
            when(authService.validate("tok-forced")).thenReturn(forcedChangeClaims);

            filter.filter(ctx);

            verify(ctx, never()).abortWith(any());
            verify(ctx).setSecurityContext(any());
        }
    }

    @Test
    void allowsNormalApiRoutesWhenMustChangePasswordIsFalse() throws IOException {
        ContainerRequestContext ctx = mockContext("GET", "/projects", "tok-normal");
        when(authService.validate("tok-normal")).thenReturn(normalClaims);

        filter.filter(ctx);

        verify(ctx, never()).abortWith(any());
        verify(ctx).setSecurityContext(any());
    }

    private static ContainerRequestContext mockContext(String method, String path, String bearer) {
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(ctx.getMethod()).thenReturn(method);
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        when(ctx.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + bearer);
        return ctx;
    }
}
