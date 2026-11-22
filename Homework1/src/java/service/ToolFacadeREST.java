/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;
import model.entities.Tool;
import authn.Secured;
@Stateless
@Path("tool")
public class ToolFacadeREST extends AbstractFacade<Tool> {

    @PersistenceContext(unitName = "Homework1PU")
    private EntityManager em;

    public ToolFacadeREST() { super(Tool.class); }

    @Override
    protected EntityManager getEntityManager() { return em; }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Tool> list(@QueryParam("author") String author,
                           @QueryParam("language") String language) {
        return em.createQuery(
            "SELECT t FROM Tool t " +
            "WHERE (:author IS NULL OR t.author.username = :author) " +
            "AND (:language IS NULL OR t.language = :language) " +
            "ORDER BY t.name", Tool.class)
            .setParameter("author", author)
            .setParameter("language", language)
            .getResultList();
    }
}