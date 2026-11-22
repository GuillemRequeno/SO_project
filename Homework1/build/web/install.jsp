<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ page import = "java.sql.*" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Database SQL Load</title>
    </head>
    <style>
        .error {
            color: red;
        }
        pre {
            color: green;
        }
    </style>
    <body>
        <h2>Database SQL Load</h2>
        <%!
            /* Siguiente valor del generador de secuencia (el mismo que usa JPA). */
            long nextId(Connection con, String generator) throws SQLException {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("VALUES (NEXT VALUE FOR " + generator + ")");
                rs.next();
                long id = rs.getLong(1);
                rs.close();
                st.close();
                return id;
            }

            long insertCustomer(Connection con, String schema, String username, String password, String name) throws SQLException {
                long id = nextId(con, "CUSTOMER_GEN");
                PreparedStatement ps = con.prepareStatement("INSERT INTO " + schema
                        + ".CUSTOMER (ID, USERNAME, PASSWORD, NAME) VALUES (?, ?, ?, ?)");
                ps.setLong(1, id);
                ps.setString(2, username);
                ps.setString(3, password);
                ps.setString(4, name);
                ps.executeUpdate();
                ps.close();
                return id;
            }

            long insertTool(Connection con, String schema, String name, String description, String language,
                    String code, String version, boolean isPrivate, long authorId) throws SQLException {
                long id = nextId(con, "TOOL_GEN");
                PreparedStatement ps = con.prepareStatement("INSERT INTO " + schema
                        + ".TOOL (ID, NAME, DESCRIPTION, LANGUAGE, CODE, VERSION, ISPRIVATE, CREATIONDATE, AUTHOR_ID) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, id);
                ps.setString(2, name);
                ps.setString(3, description);
                ps.setString(4, language);
                ps.setString(5, code);
                ps.setString(6, version);
                ps.setBoolean(7, isPrivate);
                ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
                ps.setLong(9, authorId);
                ps.executeUpdate();
                ps.close();
                return id;
            }

            long insertPrompt(Connection con, String schema, String title, String description, String template,
                    boolean isPrivate, long authorId, String[] tags, long[] toolIds) throws SQLException {
                long id = nextId(con, "PROMPT_GEN");
                PreparedStatement ps = con.prepareStatement("INSERT INTO " + schema
                        + ".PROMPT (ID, TITLE, DESCRIPTION, TEMPLATE, ISPRIVATE, CREATIONDATE, AUTHOR_ID) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, id);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setString(4, template);
                ps.setBoolean(5, isPrivate);
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
                ps.setLong(7, authorId);
                ps.executeUpdate();
                ps.close();

                for (String tag : tags) {
                    PreparedStatement pt = con.prepareStatement("INSERT INTO " + schema
                            + ".PROMPT_TAGS (PROMPT_ID, TAGS) VALUES (?, ?)");
                    pt.setLong(1, id);
                    pt.setString(2, tag);
                    pt.executeUpdate();
                    pt.close();
                }
                for (long toolId : toolIds) {
                    PreparedStatement pl = con.prepareStatement("INSERT INTO " + schema
                            + ".PROMPT_TOOL (PROMPT_ID, TOOLS_ID) VALUES (?, ?)");
                    pl.setLong(1, id);
                    pl.setLong(2, toolId);
                    pl.executeUpdate();
                    pl.close();
                }
                return id;
            }
        %>
        <%
            /* Si hay algun problema, se detiene en el primer error. */
            String dbname = "sob_grup_63";
            String schema = "ROOT";

            Class.forName("org.apache.derby.jdbc.ClientDriver");
            Connection con = DriverManager.getConnection("jdbc:derby://localhost:1527/" + dbname, "root", "root");

            try {
                /* Usuarios (sob/sob es el usuario de prueba obligatorio) */
                long sob = insertCustomer(con, schema, "sob", "sob", "Test user");
                long alice = insertCustomer(con, schema, "alice", "alice", "Alice");
                long bob = insertCustomer(con, schema, "bob", "bob", "Bob");
                out.println("<pre> -> customers: sob=" + sob + " alice=" + alice + " bob=" + bob + "</pre>");

                /* Herramientas */
                long t1 = insertTool(con, schema, "word_counter", "Cuenta palabras de un texto", "python",
                        "def word_counter(text):\n    return len(text.split())", "1.0", false, alice);
                long t2 = insertTool(con, schema, "csv_reader", "Lee un CSV y devuelve las filas", "python",
                        "import csv\n\ndef csv_reader(path):\n    with open(path) as f:\n        return list(csv.reader(f))",
                        "1.2", false, bob);
                long t3 = insertTool(con, schema, "secret_tool", "Herramienta privada", "python",
                        "def secret():\n    return 42", "0.1", true, alice);
                out.println("<pre> -> tools: " + t1 + ", " + t2 + ", " + t3 + "</pre>");

                /* Prompts */
                long p1 = insertPrompt(con, schema, "Resumir texto", "Resume un texto en 3 frases",
                        "Resume el siguiente texto en 3 frases: {{text}}", false, alice,
                        new String[]{"resumen", "texto"}, new long[]{t1});
                long p2 = insertPrompt(con, schema, "Analizar CSV", "Analiza un fichero CSV",
                        "Analiza los datos del CSV {{path}} y describe tendencias", false, bob,
                        new String[]{"datos", "analisis"}, new long[]{t2});
                long p3 = insertPrompt(con, schema, "Prompt privado", "Solo para usuarios registrados",
                        "Contenido confidencial: {{secret}}", true, alice,
                        new String[]{"resumen"}, new long[]{t3});
                long p4 = insertPrompt(con, schema, "Sin herramientas", "Prompt sin tools asociadas",
                        "Escribe un poema sobre {{tema}}", false, sob,
                        new String[]{"creativo"}, new long[]{});
                out.println("<pre> -> prompts: " + p1 + ", " + p2 + ", " + p3 + ", " + p4 + "</pre>");
            } catch (SQLException e) {
                out.println("<span class='error'>Error inserting data: " + e.getMessage() + "</span>");
            } finally {
                con.close();
            }
        %>
        <button onclick="window.location='<%=request.getSession().getServletContext().getContextPath()%>'">Go home</button>
    </body>
</html>
