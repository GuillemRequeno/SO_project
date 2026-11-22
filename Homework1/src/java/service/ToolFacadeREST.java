package service;

import authn.AuthHelper;
import authn.Secured;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.entities.Customer;
import model.entities.Tool;

@Stateless
@Path("tool")
public class ToolFacadeREST extends AbstractFacade<Tool> {

    @PersistenceContext(unitName = "Homework1PU")
    private EntityManager em;

    public ToolFacadeREST() {
        super(Tool.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** JSON que espera el POST. */
    public static class ToolInput {
        public String name;
        public String description;
        public String language;      // por defecto "python"
        public String code;
        public String version;
        public Boolean isPrivate;
        public String author;        // username del autor
    }

    // ------------------------------------------------------------------
    // GET /tool?author=&language=
    // ------------------------------------------------------------------
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> list(@QueryParam("author") String author,
                                          @QueryParam("language") String language) {
        boolean hasAuthor = author != null && !author.isEmpty();
        boolean hasLanguage = language != null && !language.isEmpty();

        // Consulta dinamica: el filtrado se hace en la base de datos
        StringBuilder jpql = new StringBuilder("SELECT t FROM Tool t WHERE 1 = 1");
        if (hasAuthor) {
            jpql.append(" AND t.author.username = :author");
        }
        if (hasLanguage) {
            jpql.append(" AND t.language = :language");
        }
        jpql.append(" ORDER BY t.name");

        TypedQuery<Tool> q = em.createQuery(jpql.toString(), Tool.class);
        if (hasAuthor) {
            q.setParameter("author", author);
        }
        if (hasLanguage) {
            q.setParameter("language", language);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tool t : q.getResultList()) {
            result.add(JsonViews.tool(t));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // GET /tool/{id}
    // ------------------------------------------------------------------
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findTool(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Tool t = em.find(Tool.class, id);
        if (t == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // No lleva @Secured: solo las privadas exigen autenticacion
        if (t.isIsPrivate() && !AuthHelper.isAuthenticated(em, headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(JsonViews.toolFull(t)).build();
    }

    // ------------------------------------------------------------------
    // POST /tool
    // ------------------------------------------------------------------
    @POST
    @Secured
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTool(ToolInput in, @Context UriInfo uriInfo) {
        if (in == null) {
            return badRequest("Falta el cuerpo JSON");
        }
        if (in.name == null || in.name.trim().isEmpty()) {
            return badRequest("El nombre no puede estar vacio");
        }
        if (in.code == null || in.code.trim().isEmpty()) {
            return badRequest("El codigo Python es obligatorio");
        }
        if (in.author == null || in.author.trim().isEmpty()) {
            return badRequest("Falta el autor");
        }
        Customer author = AuthHelper.findCustomer(em, in.author);
        if (author == null) {
            return badRequest("El autor no existe: " + in.author);
        }

        Tool t = new Tool();
        t.setName(in.name.trim());
        t.setDescription(in.description);
        t.setCode(in.code);
        t.setVersion(in.version);
        if (in.language == null || in.language.trim().isEmpty()) {
            t.setLanguage("python");
        } else {
            t.setLanguage(in.language.trim().toLowerCase());
        }
        t.setIsPrivate(in.isPrivate != null && in.isPrivate);
        t.setCreationDate(new Date());
        t.setAuthor(author);

        em.persist(t);
        em.flush(); // asegura que el id ya esta generado

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", t.getId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(t.getId())).build())
                .entity(body)
                .build();
    }

    private Response badRequest(String msg) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", msg);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }
}
