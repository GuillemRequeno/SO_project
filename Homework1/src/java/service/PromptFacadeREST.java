package service;

import authn.Secured;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import model.entities.Customer;
import model.entities.Prompt;
import model.entities.Tool;

@Stateless
@Path("prompt")
public class PromptFacadeREST extends AbstractFacade<Prompt> {

    @PersistenceContext(unitName = "Homework1PU")
    private EntityManager em;

    public PromptFacadeREST() {
        super(Prompt.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** JSON que espera el POST. */
    public static class PromptInput {
        public String title;
        public String description;
        public String template;
        public Boolean isPrivate;
        public String author;        // username del autor
        public List<String> tags;
        public List<Long> tools;     // ids de las herramientas
    }

    // ------------------------------------------------------------------
    // GET /prompt?author=&tag=
    // ------------------------------------------------------------------
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> listPrompts(@QueryParam("author") String author,
                                                 @QueryParam("tag") String tag) {
        // Consulta dinámica: el filtrado se hace en la base de datos
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT p FROM Prompt p WHERE 1 = 1");
        if (author != null && !author.isEmpty()) {
            jpql.append(" AND p.author.username = :author");
        }
        if (tag != null && !tag.isEmpty()) {
            jpql.append(" AND :tag MEMBER OF p.tags");
        }
        jpql.append(" ORDER BY p.title");

        TypedQuery<Prompt> q = em.createQuery(jpql.toString(), Prompt.class);
        if (author != null && !author.isEmpty()) {
            q.setParameter("author", author);
        }
        if (tag != null && !tag.isEmpty()) {
            q.setParameter("tag", tag);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Prompt p : q.getResultList()) {
            result.add(p.isIsPrivate() ? toSummary(p) : toFull(p));
        }
        return result;
    }

    // ------------------------------------------------------------------
    // GET /prompt/{id}
    // ------------------------------------------------------------------
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findPrompt(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Prompt p = em.find(Prompt.class, id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // No lleva @Secured: solo los privados exigen autenticación
        if (p.isIsPrivate() && !isAuthenticated(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(toFull(p)).build();
    }

    // ------------------------------------------------------------------
    // POST /prompt
    // ------------------------------------------------------------------
    @POST
    @Secured
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPrompt(PromptInput in, @Context UriInfo uriInfo) {
        if (in == null) {
            return badRequest("Falta el cuerpo JSON");
        }
        if (in.title == null || in.title.trim().isEmpty()) {
            return badRequest("El titulo no puede estar vacio");
        }
        if (in.template == null || in.template.trim().isEmpty()) {
            return badRequest("El contenido (template) es obligatorio");
        }
        if (in.author == null || in.author.trim().isEmpty()) {
            return badRequest("Falta el autor");
        }
        Customer author = findCustomer(in.author);
        if (author == null) {
            return badRequest("El autor no existe: " + in.author);
        }

        Set<Tool> tools = new HashSet<>();
        if (in.tools != null) {
            for (Long toolId : in.tools) {
                Tool t = (toolId == null) ? null : em.find(Tool.class, toolId);
                if (t == null) {
                    return badRequest("La herramienta no existe: " + toolId);
                }
                tools.add(t);
            }
        }

        Prompt p = new Prompt();
        p.setTitle(in.title.trim());
        p.setDescription(in.description);
        p.setTemplate(in.template);
        p.setIsPrivate(in.isPrivate != null && in.isPrivate);
        p.setCreationDate(new Date());
        p.setAuthor(author);
        p.setTags(in.tags != null ? new HashSet<>(in.tags) : new HashSet<String>());
        p.setTools(tools);

        em.persist(p);
        em.flush(); // asegura que el id ya esta generado

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", p.getId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(p.getId())).build())
                .entity(body)
                .build();
    }

    // ------------------------------------------------------------------
    // GET /prompt/{id}/tools
    // ------------------------------------------------------------------
    @GET
    @Path("{id}/tools")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTools(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Prompt p = em.find(Prompt.class, id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (p.isIsPrivate() && !isAuthenticated(headers)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (p.getTools().isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tool t : p.getTools()) {
            result.add(toolMap(t));
        }
        return Response.ok(result).build();
    }

    // ------------------------------------------------------------------
    // PUT /prompt/{id}/tools   (cuerpo: [1, 2, 3])
    // ------------------------------------------------------------------
    @PUT
    @Path("{id}/tools")
    @Secured
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTools(@PathParam("id") Long id, List<Long> toolIds) {
        Prompt p = em.find(Prompt.class, id);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (toolIds == null) {
            return badRequest("Falta la lista de identificadores");
        }
        Set<Tool> tools = new HashSet<>();
        for (Long toolId : toolIds) {
            Tool t = (toolId == null) ? null : em.find(Tool.class, toolId);
            if (t == null) {
                return badRequest("La herramienta no existe: " + toolId);
            }
            tools.add(t);
        }
        p.setTools(tools);
        return Response.noContent().build();
    }

    // ==================================================================
    // Auxiliares
    // ==================================================================
    private Response badRequest(String msg) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", msg);
        return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
    }

    private Customer findCustomer(String username) {
        List<Customer> list = em.createNamedQuery("Customer.findUser", Customer.class)
                .setParameter("username", username)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    /** Comprueba a mano la cabecera Authorization (Basic). */
    private boolean isAuthenticated(HttpHeaders headers) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Basic ")) {
            return false;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(auth.substring(6).trim()),
                    StandardCharsets.UTF_8);
            int i = decoded.indexOf(':');
            if (i < 0) {
                return false;
            }
            Customer c = findCustomer(decoded.substring(0, i));
            return c != null && c.getPassword().equals(decoded.substring(i + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Resumen para prompts privados. */
    private Map<String, Object> toSummary(Prompt p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("author", p.getAuthor().getUsername());
        m.put("creationDate", p.getCreationDate());
        return m;
    }

    /** Informacion completa. */
    private Map<String, Object> toFull(Prompt p) {
        Map<String, Object> m = toSummary(p);
        m.put("description", p.getDescription());
        m.put("template", p.getTemplate());
        m.put("tags", new ArrayList<>(p.getTags()));
        m.put("private", p.isIsPrivate());
        List<Long> toolIds = new ArrayList<>();
        for (Tool t : p.getTools()) {
            toolIds.add(t.getId());
        }
        m.put("tools", toolIds);
        return m;
    }

    private Map<String, Object> toolMap(Tool t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("language", t.getLanguage());
        m.put("author", t.getAuthor().getUsername());
        if (!t.isIsPrivate()) {
            m.put("description", t.getDescription());
            m.put("version", t.getVersion());
            m.put("code", t.getCode());
        }
        return m;
    }
}