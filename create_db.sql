-- Crea la base de dades del grup 63 (si no existeix) i comprova la connexió.
-- Derby crea la BD en connectar amb create=true.
CONNECT 'jdbc:derby://localhost:1527/sob_grup_63;create=true;user=root;password=root';

-- Comprovació: ha de retornar la fila ROOT
VALUES CURRENT SCHEMA;

EXIT;
