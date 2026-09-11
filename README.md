# TVMaze Middleware - Examen Backend Java 21

API REST con Spring Boot 3.5.16, Maven y MongoDB Atlas. Implementa la busqueda de shows,
consulta del objeto completo con cache persistente y comentarios/calificaciones.
No requiere una instalacion local de MongoDB ni contenedores.

## Ejecutar

Requisitos: JDK 21 y un cluster MongoDB Atlas Free/M0. Maven Wrapper esta incluido.
La configuracion de Atlas (incluido acceso 0.0.0.0/0 solicitado por el examen)
esta en [docs/ATLAS.md](docs/ATLAS.md).

```powershell
cd "$env:USERPROFILE\Documents\prueba-tecnica-java21"
$env:MONGODB_URI = 'mongodb+srv://USUARIO:PASSWORD_URL_ENCODED@CLUSTER.mongodb.net/?retryWrites=true&w=majority&appName=tvmaze'
$env:MONGODB_DATABASE = 'tvmaze'
.\mvnw.cmd spring-boot:run
```

En Linux/macOS, exporta las mismas variables y usa `./mvnw spring-boot:run`.
Spring Boot no carga automaticamente `.env`; `.env.example` es solo una referencia.
Las credenciales se configuran en el entorno del proceso o del IDE, nunca en Git.

Atlas debe estar accesible al iniciar para crear la coleccion de comentarios y su indice.
No hay fallback a localhost ni una URI con credenciales incluidas.
`GET /actuator/health` comprueba tambien MongoDB sin exponer detalles internos.

## Contrato

| Metodo | Endpoint | Entrada | Respuesta |
| --- | --- | --- | --- |
| GET | /api/v1/search | query param search_query | 200, arreglo de shows resumidos con comments |
| GET | /api/v1/show | query param show_id | 200, objeto completo de TVMaze con comments |
| POST | /api/v1/comments | JSON show_id, comment, rating | 201, {"status":"created"} |

El contrato OpenAPI esta en [openapi.yaml](src/main/resources/static/openapi.yaml),
disponible tambien en `http://localhost:8080/openapi.yaml` al arrancar.
[requests.http](requests.http) contiene peticiones ejecutables desde un cliente REST del IDE.

```powershell
Invoke-RestMethod 'http://localhost:8080/api/v1/search?search_query=girls'
Invoke-RestMethod 'http://localhost:8080/api/v1/show?show_id=1'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/comments' -ContentType 'application/json' -Body '{"show_id":1,"comment":"Muy buena serie","rating":4.5}'
```

Busqueda:

```json
[
  {
    "id": 1,
    "name": "Ejemplo",
    "channel": "HBO",
    "summary": "<p>Resumen original</p>",
    "genres": ["Drama"],
    "comments": [{"comment": "Muy buena serie", "rating": 4.5}]
  }
]
```

`channel` prioriza `network.name`; si no existe, utiliza `webChannel.name`.
Puede ser null si no hay ninguno. `summary` preserva el HTML y los null de TVMaze.
El orden de relevancia se conserva. Una busqueda sin coincidencias retorna `[]`.
El detalle preserva todos los atributos originales, incluidos campos adicionales y `_links`,
sin envolverlos en un DTO que pueda perder informacion.

## Flujo y persistencia

```mermaid
flowchart LR
  Client --> Search["GET search"]
  Search --> TVMaze
  Search --> Comments["show_comments: consulta por lote"]
  Client --> Show["GET show"]
  Show --> Cache["show_cache: buscar ID"]
  Cache -->|miss| TVMaze
  TVMaze -->|guardar antes de responder| Cache
  Show --> Comments
  Client --> Post["POST comments"]
  Post --> Exists["validar show mediante cache/proveedor"]
  Exists --> Comments
```

- `show_cache`: `_id` es el ID del show, `payload` es el JSON completo y `cachedAt` la fecha UTC.
- `show_comments`: documento independiente por comentario, con showId, comment, rating y createdAt.
- Indice `comments_by_show` en showId, createdAt e _id; comentarios ordenados por fecha e ID.
- La busqueda obtiene todos los comentarios con una consulta `$in`, evitando N+1.
- Los comentarios se leen en cada respuesta y nunca forman parte del payload cacheado.
- No hay TTL: cualquier ID ya registrado satisface la cache conforme al enunciado.
- Un error MongoDB devuelve 503; una escritura fallida no se reporta como exitosa.

La cache es persistente y puede quedar desactualizada respecto a TVMaze.
La renovacion y los bloqueos distribuidos quedan fuera del examen; solicitudes concurrentes
para un ID ausente pueden consumir TVMaze mas de una vez, pero comparten una clave unica.
Cada POST crea un comentario independiente; no se deduplican reintentos de clientes.
No se implementa paginacion de comentarios porque el contrato solicita el arreglo completo.

## Validacion y errores

`search_query`: no vacio, maximo 200 caracteres. Se recortan espacios exteriores.
`show_id`: entero positivo de 64 bits; se rechazan fracciones.
`comment`: no vacio, maximo 2000 caracteres, con espacios exteriores recortados.
`rating`: numero entre 0 y 5 inclusive; se admiten decimales.
Antes de insertar un comentario se valida que el show exista usando cache/proveedor.
No existen endpoints para editar o borrar comentarios porque no fueron solicitados.

Errores centralizados `application/problem+json` con code, timestamp y requestId:

| HTTP | Caso |
| --- | --- |
| 400 | Parametros, JSON o validacion invalidos |
| 404 | Show inexistente |
| 502 | Respuesta invalida o fallo de TVMaze |
| 503 | Fallo MongoDB o limite de peticiones de TVMaze |
| 504 | Timeout de TVMaze |
| 500 | Error inesperado sin detalles internos en la respuesta |

Si TVMaze responde 429, el middleware devuelve 503 con `Retry-After: 10`.
El cliente puede reintentar pasado ese intervalo; no hay reintentos automaticos que multipliquen
las llamadas al proveedor.

## SOLID, i18n y logging

Dominio y casos de uso no dependen de Spring ni MongoDB.
Los puertos ShowSearchProvider, ShowProvider, ShowCache, CommentReader y CommentWriter
separan los contratos de busqueda, detalle, cache, lectura y escritura.
Los adaptadores HTTP/MongoDB implementan esos contratos y ApplicationConfig ensambla las dependencias.
Los controladores solo traducen el contrato HTTP; los servicios coordinan las operaciones.

`Accept-Language: es` (predeterminado) o `en` selecciona los mensajes.
Los logs operativos usan SLF4J/Logback y eventos estables en ingles.
`X-Request-ID` correlaciona respuesta, errores y logs; si no es valido, se genera un UUID.
Los logs de solicitudes incluyen metodo, estado y duracion, sin registrar cuerpos o credenciales.
Con APP_LOG_LEVEL=DEBUG se observan los cache hit/miss.

## Variables

| Variable | Predeterminado |
| --- | --- |
| MONGODB_URI | Obligatoria, cadena de Atlas |
| MONGODB_DATABASE | tvmaze |
| EXTERNAL_API_BASE_URL | https://api.tvmaze.com |
| EXTERNAL_API_CONNECT_TIMEOUT | PT3S |
| EXTERNAL_API_READ_TIMEOUT | PT5S |
| SERVER_PORT | 8080 |
| APP_LOG_LEVEL | INFO |

MongoDB usa tres segundos para seleccion de servidor, conexion y lectura.
Los timeouts son por operacion y no representan un deadline global.

## Pruebas

```powershell
.\mvnw.cmd -B -ntp clean verify
```

JUnit 5, Mockito, MockMvc y MockRestServiceServer verifican los casos de uso,
contratos HTTP, conversion de TVMaze, cache, comentarios y errores.
Las pruebas habituales no requieren Atlas ni credenciales. GitHub Actions ejecuta esta misma
verificacion con Java 21. Las dependencias necesitan red en su primera descarga.

La prueba de integracion utiliza Atlas real y simula solo TVMaze:

```powershell
$env:ATLAS_TEST_URI = 'mongodb+srv://USUARIO:PASS@CLUSTER.mongodb.net/?retryWrites=true&w=majority'
$env:ATLAS_TEST_DATABASE = 'tvmaze_test'
.\mvnw.cmd -B -ntp verify -Patlas-integration
```

Usa un usuario con readWrite sobre `tvmaze_test`. El nombre debe comenzar con `tvmaze_test`.
La prueba crea registros con un ID aleatorio y elimina solo esos registros al finalizar.
Comprueba cache hit/miss, insercion y consulta de comentarios, JSON completo e indice real.
Este perfil falla si falta ATLAS_TEST_URI; no se omite silenciosamente.
No se ha ejecutado contra Atlas mientras no exista una conexion configurada.

## Entrega

[docs/PROGRESS.md](docs/PROGRESS.md) relaciona los avances con commits separados.
El repositorio debe ser privado y Pinwox debe aceptar la invitacion para acceder.
La provision de Atlas requiere una cuenta autenticada y no queda resuelta por compilar el codigo.

Esta API de examen no incluye autenticacion. Antes de exponerla publicamente deben protegerse
las escrituras y limitarse las peticiones.

## Fuente de datos

Datos de [TVMaze](https://www.tvmaze.com), mediante su [API oficial](https://www.tvmaze.com/api).
TVMaze publica los datos bajo CC BY-SA; se conserva el contenido original y se agrega
la informacion local de comentarios. El contrato y sus ejemplos usan HTTPS.
