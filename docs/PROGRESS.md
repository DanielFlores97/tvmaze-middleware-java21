# Avances del examen

- Base: proyecto Java 21 conservado en un commit inicial.
- A: GET /api/v1/search?search_query=...; busqueda TVMaze, proyeccion y validacion.
- B: GET /api/v1/show?show_id=...; detalle completo, incluidos campos desconocidos y valores null.
- B/cache: consulta MongoDB primero y persiste antes de responder; sin TTL ni fallback ante errores de escritura.
- Atlas: guia de provision Free/M0 en docs/ATLAS.md; requiere sesion Atlas para crear el recurso real.
- C: POST /api/v1/comments; valida show_id, comentario y rating [0,5], verifica existencia y retorna 201.
