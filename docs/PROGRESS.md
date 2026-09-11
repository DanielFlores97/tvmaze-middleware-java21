# Avances del examen

- Base: proyecto Java 21 conservado en un commit inicial.
- A: GET /api/v1/search?search_query=...; busqueda TVMaze, proyeccion y validacion.
- B: GET /api/v1/show?show_id=...; detalle completo, incluidos campos desconocidos y valores null.
- B/cache: consulta MongoDB primero y persiste antes de responder; sin TTL ni fallback ante errores de escritura.
- Atlas: guia de provision Free/M0 en docs/ATLAS.md; requiere sesion Atlas para crear el recurso real.
- C: POST /api/v1/comments; valida show_id, comentario y rating [0,5], verifica existencia y retorna 201.
- A/comments: busqueda enriquecida mediante una sola consulta MongoDB por lote; comments=[] cuando no hay comentarios.
- B/comments: detalle completo enriquecido con comentarios frescos, incluso en cache hit; el payload cacheado no se modifica.

## Verificacion final

- Maven Wrapper clean verify: BUILD SUCCESS, 52 pruebas, sin fallos ni errores.
- Perfil atlas-integration: implementado y compilado; no ejecutado por falta de ATLAS_TEST_URI.
- Contrato OpenAPI y requests.http actualizados con los tres endpoints.
- CI de GitHub Actions preparada para Java 21.

## Commits funcionales

| Punto | Commit |
| --- | --- |
| Base | 2bbfe2f |
| A / search | 53b1f27 |
| B / show completo | 24dfa18 |
| B / cache Atlas | 9be3bdf |
| C / comments | 258dba0 |
| A / comentarios en search | 0f8fc0b |
| B / comentarios en show | 04803b9 |

La provision real de Atlas sigue pendiente de una cuenta autenticada.
