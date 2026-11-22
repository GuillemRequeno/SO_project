/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.entities.Customer;

@Stateless
@Path("customer")
public class CustomerFacadeREST extends AbstractFacade<Customer> {

    @PersistenceContext(unitName = "Homework1PU")
    private EntityManager em;

    public CustomerFacadeREST() {
        super(Customer.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> list() {
        List<Customer> customers = em.createQuery("SELECT c FROM Customer c ORDER BY c.username",
                Customer.class).getResultList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Customer c : customers) {
            // Nunca se expone la password
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("username", c.getUsername());
            m.put("name", c.getName());
            result.add(m);
        }
        return result;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> find(@PathParam("id") Long id) {
        Customer c = em.find(Customer.class, id);
        if (c == null) {
            throw new NotFoundException();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", c.getId());
        result.put("username", c.getUsername());
        result.put("name", c.getName());

        Map<String, Object> links = new LinkedHashMap<>();

        List<Long> lastPrompt = em.createQuery(
                "SELECT p.id FROM Prompt p WHERE p.author.id = :id ORDER BY p.creationDate DESC, p.id DESC",
                Long.class).setParameter("id", id).setMaxResults(1).getResultList();
        if (!lastPrompt.isEmpty()) {
            links.put("last-prompt-added", Map.of("href", "/prompt/" + lastPrompt.get(0)));
        }

        List<Long> lastTool = em.createQuery(
                "SELECT t.id FROM Tool t WHERE t.author.id = :id ORDER BY t.creationDate DESC, t.id DESC",
                Long.class).setParameter("id", id).setMaxResults(1).getResultList();
        if (!lastTool.isEmpty()) {
            links.put("last-tool-added", Map.of("href", "/tool/" + lastTool.get(0)));
        }

        result.put("_links", links);
        return result;
    }
}