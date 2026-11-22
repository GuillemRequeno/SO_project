package authn;

import java.io.IOException;
import java.lang.reflect.Method;
import jakarta.annotation.Priority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import model.entities.Customer;

/**
 * Filtro que autentica (HTTP Basic) las peticiones a metodos anotados con
 * {@link Secured}.
 *
 * 401: sin cabecera o usuario desconocido; 400: cabecera mal formada;
 * 403: usuario correcto pero contrasena incorrecta.
 *
 * @author Marc Sanchez
 */
@Priority(Priorities.AUTHENTICATION)
@Provider
public class RESTRequestFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @PersistenceContext(unitName = "Homework1PU")
    private EntityManager em;

    @Override
    public void filter(ContainerRequestContext requestCtx) throws IOException {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) {
            return;
        }
        if (method.getAnnotation(Secured.class) == null) {
            return;
        }

        String header = requestCtx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isEmpty()) {
            requestCtx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String[] cred = AuthHelper.decodeBasic(header);
        if (cred == null) {
            requestCtx.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        Customer c = AuthHelper.findCustomer(em, cred[0]);
        if (c == null) {
            requestCtx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }
        if (!c.getPassword().equals(cred[1])) {
            requestCtx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
        }
    }
}
