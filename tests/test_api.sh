#!/bin/bash
# Juego de pruebas con curl para la API REST (SOB Practica 1).
# Uso: ./test_api.sh [BASE_URL]
# Requisito: haber ejecutado install.jsp (usuarios sob/alice/bob y datos de prueba).

BASE="${1:-http://localhost:8080/Homework1/rest/api/v1}"
PASS=0
FAIL=0

# check "descripcion" codigo_esperado metodo url [curl args...]
check() {
    desc="$1"
    expected="$2"
    shift 2
    code=$(curl -s -o /tmp/sob_body.txt -w "%{http_code}" -X "$@")
    if [ "$code" = "$expected" ]; then
        echo "OK   [$code] $desc"
        PASS=$((PASS + 1))
    else
        echo "FAIL [$code, esperado $expected] $desc"
        cat /tmp/sob_body.txt
        echo
        FAIL=$((FAIL + 1))
    fi
}

J='Content-Type: application/json'

echo "== PROMPTS: lectura =="
check "GET prompts (lista)"                      200 GET  "$BASE/prompt"
check "GET prompts filtro author=alice"          200 GET  "$BASE/prompt?author=alice"
check "GET prompts filtro tag=resumen"           200 GET  "$BASE/prompt?tag=resumen"
check "GET prompts author+tag"                   200 GET  "$BASE/prompt?author=alice&tag=resumen"
check "GET prompt publico (1) sin login"         200 GET  "$BASE/prompt/1"
check "GET prompt inexistente"                   404 GET  "$BASE/prompt/9999"
check "GET prompt privado (3) sin login"         401 GET  "$BASE/prompt/3"
check "GET prompt privado (3) con login"         200 GET  "$BASE/prompt/3" --user sob:sob
check "GET prompt privado (3) password erronea"  401 GET  "$BASE/prompt/3" --user sob:mal
check "GET tools del prompt 1"                   200 GET  "$BASE/prompt/1/tools"
check "GET tools del prompt sin tools (4)"       404 GET  "$BASE/prompt/4/tools"
check "GET tools de prompt inexistente"          404 GET  "$BASE/prompt/9999/tools"
check "GET tools de prompt privado sin login"    401 GET  "$BASE/prompt/3/tools"

echo "== PROMPTS: escritura =="
check "POST prompt sin login"                    401 POST "$BASE/prompt" -H "$J" -d '{"title":"x","template":"t","author":"sob"}'
check "POST prompt password erronea"             403 POST "$BASE/prompt" --user sob:mal -H "$J" -d '{"title":"x","template":"t","author":"sob"}'
check "POST prompt titulo vacio"                 400 POST "$BASE/prompt" --user sob:sob -H "$J" -d '{"title":"","template":"t","author":"sob"}'
check "POST prompt sin template"                 400 POST "$BASE/prompt" --user sob:sob -H "$J" -d '{"title":"x","author":"sob"}'
check "POST prompt autor inexistente"            400 POST "$BASE/prompt" --user sob:sob -H "$J" -d '{"title":"x","template":"t","author":"nadie"}'
check "POST prompt herramienta inexistente"      400 POST "$BASE/prompt" --user sob:sob -H "$J" -d '{"title":"x","template":"t","author":"sob","tools":[9999]}'
check "POST prompt correcto"                     201 POST "$BASE/prompt" --user sob:sob -H "$J" \
    -d '{"title":"Nuevo prompt","description":"d","template":"Hola {{x}}","author":"sob","isPrivate":false,"tags":["test"],"tools":[1,2]}'
echo "   respuesta: $(cat /tmp/sob_body.txt)"
NEWID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' /tmp/sob_body.txt)
check "GET del prompt recien creado"             200 GET  "$BASE/prompt/$NEWID"
check "PUT tools sin login"                      401 PUT  "$BASE/prompt/$NEWID/tools" -H "$J" -d '[1]'
check "PUT tools prompt inexistente"             404 PUT  "$BASE/prompt/9999/tools" --user sob:sob -H "$J" -d '[1]'
check "PUT tools con herramienta inexistente"    400 PUT  "$BASE/prompt/$NEWID/tools" --user sob:sob -H "$J" -d '[9999]'
check "PUT tools correcto"                       204 PUT  "$BASE/prompt/$NEWID/tools" --user sob:sob -H "$J" -d '[3]'

echo "== TOOLS =="
check "GET tools (lista)"                        200 GET  "$BASE/tool"
check "GET tools filtro author=alice"            200 GET  "$BASE/tool?author=alice"
check "GET tools filtro language=python"         200 GET  "$BASE/tool?language=python"
check "GET tools language=java (vacio)"          200 GET  "$BASE/tool?language=java"
check "GET tool publica (1) sin login"           200 GET  "$BASE/tool/1"
check "GET tool inexistente"                     404 GET  "$BASE/tool/9999"
check "GET tool privada (3) sin login"           401 GET  "$BASE/tool/3"
check "GET tool privada (3) con login"           200 GET  "$BASE/tool/3" --user sob:sob
check "POST tool sin login"                      401 POST "$BASE/tool" -H "$J" -d '{"name":"x","code":"print(1)","author":"sob"}'
check "POST tool nombre vacio"                   400 POST "$BASE/tool" --user sob:sob -H "$J" -d '{"name":"","code":"print(1)","author":"sob"}'
check "POST tool sin codigo"                     400 POST "$BASE/tool" --user sob:sob -H "$J" -d '{"name":"x","author":"sob"}'
check "POST tool autor inexistente"              400 POST "$BASE/tool" --user sob:sob -H "$J" -d '{"name":"x","code":"print(1)","author":"nadie"}'
check "POST tool correcta"                       201 POST "$BASE/tool" --user sob:sob -H "$J" \
    -d '{"name":"hello","description":"d","code":"print(1)","version":"1.0","author":"sob"}'
echo "   respuesta: $(cat /tmp/sob_body.txt)"

echo "== CUSTOMERS =="
check "GET customers"                            200 GET  "$BASE/customer"
if grep -qi '"password"' /tmp/sob_body.txt; then
    echo "FAIL la lista de customers expone la password"
    FAIL=$((FAIL + 1))
else
    echo "OK   la lista de customers no expone la password"
    PASS=$((PASS + 1))
fi
check "GET customer 1"                           200 GET  "$BASE/customer/1"
echo "   respuesta: $(cat /tmp/sob_body.txt)"
if grep -qi '"password"' /tmp/sob_body.txt; then
    echo "FAIL customer/1 expone la password"
    FAIL=$((FAIL + 1))
fi
check "GET customer inexistente"                 404 GET  "$BASE/customer/9999"

echo
echo "Resultado: $PASS OK, $FAIL FAIL"
[ "$FAIL" -eq 0 ]
