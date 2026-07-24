package br.ufmt.periscope.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS activator. Resources are served under {@code /periscope/rest/*}.
 * Role checks ({@code @RolesAllowed}) are enabled via
 * {@code resteasy.role.based.security} in {@code web.xml}.
 */
@ApplicationPath("/rest")
public class JaxRsActivator extends Application {
    /* empty: classpath scanning discovers resources/providers */
}
