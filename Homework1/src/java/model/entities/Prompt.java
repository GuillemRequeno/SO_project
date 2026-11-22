/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model.entities;
import jakarta.persistence.*;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.*;
@Entity
@XmlRootElement
public class Prompt implements Serializable {

    @Id
    @SequenceGenerator(name = "Prompt_Gen", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Prompt_Gen")
    private Long id;

    @Column(nullable = false)
    private String title;
    private String description;
    @Lob
    private String template;
    private boolean isPrivate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @ManyToOne(optional = false)
    private Customer author;

    @ElementCollection
    private Set<String> tags = new HashSet<>();

    @ManyToMany
    private Set<Tool> tools = new HashSet<>();

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Customer getAuthor() {
        return author;
    }

    public void setAuthor(Customer author) {
        this.author = author;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<Tool> getTools() {
        return tools;
    }

    public void setTools(Set<Tool> tools) {
        this.tools = tools;
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Prompt)) {
            return false;
        }
        Prompt other = (Prompt) object;
        if (this.id == null || other.id == null) {
            return this == other;
        }
        return this.id.equals(other.id);
    }
}
