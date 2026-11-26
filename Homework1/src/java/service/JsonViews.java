package service;

import java.util.LinkedHashMap;
import java.util.Map;
import model.entities.Tool;


public final class JsonViews {

    private JsonViews() {
    }

    /** Resumen de una herramienta (lo unico visible de una privada). */
    public static Map<String, Object> toolSummary(Tool t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("author", t.getAuthor().getUsername());
        m.put("language", t.getLanguage());
        return m;
    }

    /** Informacion completa de una herramienta. */
    public static Map<String, Object> toolFull(Tool t) {
        Map<String, Object> m = toolSummary(t);
        m.put("description", t.getDescription());
        m.put("code", t.getCode());
        m.put("version", t.getVersion());
        m.put("isPrivate", t.isIsPrivate());
        m.put("creationDate", t.getCreationDate());
        return m;
    }

    /** Resumen si la herramienta es privada, completa si es publica. */
    public static Map<String, Object> tool(Tool t) {
        if (t.isIsPrivate()) {
            return toolSummary(t);
        }
        return toolFull(t);
    }
}
