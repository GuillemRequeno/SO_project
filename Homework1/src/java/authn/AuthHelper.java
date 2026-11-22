package authn;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.HttpHeaders;
import model.entities.Customer;

/**
 * Utilidades de autenticacion HTTP Basic compartidas por el filtro
 * (@Secured) y por los recursos que autentican "a mano" (p. ej. GET de un
 * recurso privado, donde no se puede usar @Secured porque los publicos
 * no exigen credenciales).
 */
public final class AuthHelper {

    private static final String PREFIX = "Basic ";

    private AuthHelper() {
    }

    /** Devuelve el usuario con ese username o null si no existe. */
    public static Customer findCustomer(EntityManager em, String username) {
        List<Customer> list = em.createNamedQuery("Customer.findUser", Customer.class)
                .setParameter("username", username)
                .getResultList();
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Devuelve {usuario, password} decodificados de la cabecera Authorization,
     * o null si la cabecera falta o esta mal formada.
     */
    public static String[] decodeBasic(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(PREFIX)) {
            return null;
        }
        try {
            byte[] raw = Base64.getDecoder().decode(authHeader.substring(PREFIX.length()).trim());
            String decoded = new String(raw, StandardCharsets.UTF_8);
            int i = decoded.indexOf(':');
            if (i < 0) {
                return null;
            }
            return new String[]{decoded.substring(0, i), decoded.substring(i + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** true si la cabecera Authorization trae credenciales validas. */
    public static boolean isAuthenticated(EntityManager em, HttpHeaders headers) {
        String[] cred = decodeBasic(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (cred == null) {
            return false;
        }
        Customer c = findCustomer(em, cred[0]);
        if (c == null) {
            return false;
        }
        return c.getPassword().equals(cred[1]);
    }
}
