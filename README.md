# Prueba tecnica: Java 21 + MongoDB Atlas

Base funcional con Spring Boot 3.5.16, Maven, consumo HTTP con RestClient,
API REST, cache-aside en Atlas, SLF4J/Logback, i18n y errores Problem Detail.
El recurso de ejemplo es una publicacion de JSONPlaceholder; se puede sustituir el proveedor
sin modificar el caso de uso. No requiere Docker ni una instalacion local de MongoDB.

## Arranque con Atlas

Requisitos: JDK 21 y un cluster MongoDB Atlas. Maven Wrapper viene incluido.

1. En Atlas, crea un usuario de **base de datos** con permiso readWrite sobre
   `prueba_tecnica`. La aplicacion necesita crear la coleccion y su indice TTL.
2. En Network Access, autoriza la IP desde la que ejecutaras la aplicacion.
3. En Connect > Drivers, copia la URI. Sustituye usuario, password y host.
   Codifica los caracteres especiales de las credenciales mediante percent-encoding.
4. Configura las variables en la misma terminal y arranca:

```powershell
cd "$env:USERPROFILE\Documents\prueba-tecnica-java21"
$env:MONGODB_URI = 'mongodb+srv://USUARIO:PASSWORD_URL_ENCODED@CLUSTER.mongodb.net/?retryWrites=true&w=majority&appName=prueba-tecnica'
$env:MONGODB_DATABASE = 'prueba_tecnica'
.\mvnw.cmd spring-boot:run
```

En Linux/macOS: exporta las mismas variables y ejecuta `sh mvnw spring-boot:run`.
El archivo `.env.example` es una referencia: Spring Boot no carga archivos .env automaticamente.
No guardes la URI real en el repositorio ni la compartas en logs o peticiones HTTP.

La URI es obligatoria, sin fallback local. El arranque exige conexion a Atlas para crear
el indice: una URI incorrecta, IP no autorizada o falta de permisos impide iniciar.
No se ha configurado un cluster ni se incluyen credenciales.

## API

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| GET | /api/v1/posts/{id} | Publicacion; consulta Atlas y, ante un miss, al proveedor |
| DELETE | /api/v1/posts/{id}/cache | 204; invalida la entrada, sin borrar el recurso externo |
| GET | /actuator/health | Salud, incluida la conexion a MongoDB |

Los IDs deben ser positivos. `requests.http` incluye peticiones para un cliente REST del IDE.

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/posts/1
Invoke-RestMethod -Method Delete http://localhost:8080/api/v1/posts/1/cache
Invoke-RestMethod http://localhost:8080/actuator/health
```

## Cache

Coleccion `post_cache`, clave unica `_id` por publicacion, valor `post` y fecha `expiresAt`.
Spring Data crea `post_cache_expiry` con `expireAfterSeconds: 0`.
El TTL por defecto es cinco minutos desde la escritura; leer no renueva la expiracion.
Cada lectura verifica la fecha porque la eliminacion fisica de TTL es asincrona.

Si falla una lectura/escritura de cache durante la ejecucion, se permite obtener la respuesta
del proveedor. Una invalidacion fallida devuelve 503. No se cachean errores ni resultados
vencidos. Si Atlas falla, health puede devolver DOWN aunque algunas consultas sigan funcionando.
Varias solicitudes simultaneas para una clave ausente pueden consultar al proveedor en paralelo.
No se implementan bloqueos distribuidos ni stale-while-revalidate.

## Configuracion

| Variable | Valor por defecto |
| --- | --- |
| MONGODB_URI | Obligatoria, URI de Atlas |
| MONGODB_DATABASE | prueba_tecnica |
| CACHE_TTL | PT5M, minimo PT1S |
| EXTERNAL_API_BASE_URL | https://jsonplaceholder.typicode.com |
| EXTERNAL_API_CONNECT_TIMEOUT | PT3S |
| EXTERNAL_API_READ_TIMEOUT | PT5S |
| SERVER_PORT | 8080 |
| APP_LOG_LEVEL | INFO |

El cliente MongoDB usa tres segundos para seleccion de servidor, conexion y lectura.
El timeout del proveedor se traduce a 504; sus fallos y respuestas invalidas a 502.
Estos limites de operaciones no representan un deadline global de la peticion.

## SOLID y estructura

```text
com.example.pruebatecnica
  domain                 Post y errores de aplicacion, sin dependencias Spring
  application            Caso de uso PostService
    port                 Contratos PostProvider y PostCache
  infrastructure
    http                 Adaptador de JSONPlaceholder
    cache                Adaptador MongoDB y documento TTL
    web                  Controlador, errores y filtro de logging
  config                 Inyeccion de dependencias, HTTP, idiomas y propiedades
```

Responsabilidad unica: cada adaptador gestiona su tecnologia. Inversion de dependencias:
el servicio depende de interfaces y se ensambla en configuracion.
Los contratos son pequenos; otro proveedor o cache puede implementarlos sin cambiar
el servicio. Los tests del caso de uso usan sustitutos de esos contratos.

## Mensajes, logging y excepciones

`Accept-Language: es` (por defecto) o `en` selecciona los mensajes en
`src/main/resources/i18n/messages*.properties`. Los datos del proveedor no se traducen.
Los logs operativos usan eventos estables en ingles, SLF4J y Logback en consola.
Para ver hit/miss, configura `APP_LOG_LEVEL=DEBUG`.

Cada peticion recibe `X-Request-ID`; el mismo valor aparece en MDC y en errores.
Se acepta un ID entrante de 1 a 64 caracteres alfanumericos, punto, guion o guion bajo.
Se registra metodo, estado y duracion, sin cuerpos, credenciales o query strings.

Los errores usan `application/problem+json`, con `status`, `title`, `detail`,
`type`, `code`, `timestamp` y `requestId`.
Codigos relevantes: 400 validacion, 404 recurso inexistente, 502 proveedor,
503 invalidacion de cache, 504 timeout y 500 error inesperado.
Los errores inesperados conservan stack trace en logs del servidor.

## Verificacion

```powershell
.\mvnw.cmd clean verify
```

Las pruebas usan JUnit 5, Mockito, MockMvc y MockRestServiceServer. Cubren hit/miss,
expiracion, fallos de cache, invalidacion, errores del proveedor, validacion,
traducciones y correlacion. No requieren red, Atlas ni credenciales una vez descargadas
las dependencias. No sustituyen una prueba real contra Atlas.

Para verificar Atlas: arranca con tu URI, consulta /posts/1 dos veces con logging DEBUG,
comprueba miss/hit, inspecciona `post_cache` y el indice en Atlas e invalida la entrada.
Con `CACHE_TTL=PT5S`, consulta de nuevo tras cinco segundos: debe volver al proveedor
aunque MongoDB aun conserve el documento vencido.

La API de ejemplo no incluye autenticacion; el endpoint de invalidacion debe protegerse
si se publica fuera del entorno de la prueba.

## Referencias

- [Requisitos de Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Conexion a Atlas](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/)
- [Indices TTL](https://www.mongodb.com/docs/manual/core/index-ttl/)
- [API de ejemplo](https://jsonplaceholder.typicode.com/guide/)
