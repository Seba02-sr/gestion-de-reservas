# CLAUDE.md

Guía para trabajar en este repo (TP de *Desarrollo de Aplicaciones en la Nube*, UTN FRSF). Fork de la cátedra: https://github.com/dan-frsf/tp-2025 (remote `catedra`, solo referencia).

## Estructura del repo

Monorepo Maven multi-módulo (Spring Boot, Java 21) + frontend.

```
pom.xml                      # parent (tp2025)
common/dan-common-lib/       # librería compartida (DTOs/eventos de mensajería)
common/dan-spring-gateway/   # API Gateway (Spring Cloud Gateway WebMVC)  -> 8080
common/dan-eureka-server/    # Service discovery (Eureka Server)            -> 8761
services/user-svc/           # usuarios (MySQL)            -> 8080 interno
services/reservas-svc/       # reservas (MongoDB)          -> 8080 interno
services/gestion-svc/        # hoteles/habitaciones/tarifas (PostgreSQL) -> 8080 interno
frontend/                    # React + Vite
docker/                      # TODO lo de Docker (consigna de entrega): compose, Dockerfiles y configs
  docker-compose.yml         #   compose principal (perfiles observability, tools, dev)
  docker-compose.dev-ports.yml  # override opcional: publica las bases en el host
  backend.Dockerfile         #   multi-etapa (Maven+JDK21 -> un target JRE21 por servicio)
  frontend.Dockerfile        #   Node -> nginx sin root
  config/                    #   rabbitmq, postgres/initdb, mysql, pgadmin, phpmyadmin, otel, tempo, loki, prometheus, grafana
docker-compose.yml           # solo `include` de docker/docker-compose.yml
docs/                        # documentación (README para quien corrige en la raíz), docs/postman con la colección única
ETAPA01.md, ETAPA02.md       # enunciados; PRACTICA_02.pdf, TP_PARTE_02.pdf
```

**El compose vigente es `docker/docker-compose.yml`** (el de la raíz solo lo incluye con `include`). Antes había un `infra/docker-compose.yml` legado y Dockerfiles por servicio que exigían `target/*.jar`: se eliminaron para que haya una sola fuente de verdad.

Mensajería asíncrona: RabbitMQ (gestion-svc publica eventos de hoteles/habitaciones/tarifas; reservas-svc los consume). Las clases del evento viven en `dan-common-lib`.

## Requisitos: JDK 21

El proyecto requiere **JDK 21**. En esta máquina el `java` del PATH puede ser 1.8 y `JAVA_HOME` puede apuntar a otro JDK, así que hay que setearlo por sesión.

PowerShell (solo para la sesión actual):
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version   # debe decir 21
```

Git Bash:
```bash
export JAVA_HOME="C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"
export PATH="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin:$PATH"
```

## Compilar y levantar

```bash
./mvnw clean install                 # compila todo y corre tests
./mvnw spotless:apply                # formatear (obligatorio antes de commitear)

# Docker: no hace falta compilar antes, el Dockerfile compila el reactor dentro de la imagen
docker compose -f docker/docker-compose.yml up -d --build    # app completa, sin .env ni perfiles
docker compose -f docker/docker-compose.yml down             # o `down -v` para borrar volúmenes
```

**Sin perfiles** levanta la app completa (MySQL, PostgreSQL, MongoDB, RabbitMQ, Eureka, user/gestion/reservas, gateway y el frontend de producción en nginx, 5173). Perfiles opcionales: `observability` (Collector, Tempo, Loki, Prometheus, Grafana), `tools` (pgAdmin, phpMyAdmin, Mongo Express) y `dev` (servicio `frontend-dev`, Vite en caliente en 5174). Todas las variables tienen valor por defecto `${VAR:-valor}` (no hace falta `.env`). `DEMO_DATOS_HABILITADO` (true en Docker) carga los datos de demo (ver "Datos de demo").

Tests de un servicio: `./mvnw -pl services/gestion-svc test`.

> **Windows/OneDrive:** el repo está dentro de OneDrive y a veces `clean` falla con "Failed to delete ...\target\classes\..." por un lock (OneDrive o el language server de Java). Solución: borrar los `target/` a mano y reintentar.

### MySQL (user-svc): el esquema lo crea Hibernate
El compose **no monta `docker/config/mysql/initdb`** en el servicio `mysql` (solo el volumen `mysql_data`), así que las tablas las crea Hibernate con `spring.jpa.hibernate.ddl-auto=update`. `docker/config/mysql/initdb/01_schema.sql` es solo referencia (alineado con las entidades) y su `ON DELETE CASCADE` **no existe** en la base real: el borrado de un huésped elimina sus tarjetas únicamente por el cascade JPA (`Huesped.tarjetaCredito`: `cascade = ALL` + `orphanRemoval`), verificado en `HuespedBorradoCascadaTest` (H2). No hay que depender de FKs de la base para esa regla.

### initdb y volúmenes (PostgreSQL y demás bases que sí montan initdb)
Los scripts de `docker/config/*/initdb` (p. ej. `docker/config/postgres/initdb/01_schema.sql`) solo se ejecutan cuando el volumen de la base está vacío. **Si cambia un script de initdb hay que recrear el volumen**: `docker compose -f docker/docker-compose.yml down -v` (borra todo) o borrar solo el volumen de esa base (`docker volume ls` → `docker volume rm <proyecto>_postgres_data`, `..._mysql_data`, `..._mongo_data`).

### PostgreSQL (gestion-svc) y mensajería de habitaciones
- `docker/config/postgres/initdb/01_schema.sql` **sí** se monta y gestion-svc corre con `ddl-auto=none`: la columna `habitacion.disponible` y la restricción `UNIQUE (id_hotel, numero)` (`uq_habitacion_hotel_numero`) solo existen si se recrea el volumen (`docker compose -f docker/docker-compose.yml down -v` o `docker volume rm <proyecto>_postgres_data`).
- gestion-svc publica en el exchange topic `dan.exchange` con routing key `dan.habitacion.<accion>` (`crear`, `actualizar`, `eliminar`, `cerrar-hotel`); reservas-svc escucha la cola `habitacion.topic` (binding `dan.habitacion.#`, ver `docker/config/rabbitmq/definitions.json`). Propiedades: `rabbitmq.exchange` y `rabbitmq.routingkey.prefix`.
- Los mensajes se envían **después del commit** (`@TransactionalEventListener(AFTER_COMMIT)` en `HabitacionEventPublisher`); si la transacción hace rollback no sale nada. Eventos: `CREAR`/`ACTUALIZAR_DATOS`/`ELIMINAR` (alta, modificación y baja de habitación), `ACTUALIZAR_DATOS` por cada habitación cuando cambia el hotel o sus amenities, y `CERRAR_HOTEL` (un mensaje con `hotelId`, `habitacionIds` y `fechaCierre`; reservas-svc por ahora solo lo registra: falta crear las reservas CERRADA).

### Tarifas (gestion-svc)
- Solo POST y DELETE. Vigente en una fecha = `fechaInicio <= fecha` y (`fechaFin` null o `fechaFin >= fecha`). `tarifa.fecha_fin` es nullable y la tarifa tiene `nombre` y `especial` (promoción): hace falta recrear el volumen de postgres.
- Sin fechas: la actual termina ayer y la nueva rige desde hoy. Con fechas (promo): la actual termina el día anterior, se crea la promo y una continuación desde el día siguiente con el precio de la tarifa cortada. No se permiten promos superpuestas (409). Una tarifa común con promos programadas rige hasta el día anterior a la próxima promo, las tarifas posteriores a las promos pasan al nuevo precio y las futuras previas a la próxima promo se absorben.
- DELETE: 409 si es la única del tipo; si era la vigente (o futura), la anterior extiende su `fechaFin` al de la borrada (sin huecos).
- "Hoy" sale de un `Clock` con zona `app.zone` (Buenos Aires por defecto). `ACTUALIZAR_PRECIO` (`dan.habitacion.precio`) se publica al cambiar el precio de hoy y, para cambios futuros, lo publica la tarea diaria `TarifaPrecioScheduler` (cron `tarifas.cron-cambio-precio`, por defecto `0 5 0 * * *`), que además corre una vez al arrancar la aplicación (`ApplicationReadyEvent`).

### reservas-svc (MongoDB): estados, índices y fecha simulada
- Decisiones de diseño (regla de pagos, disponibilidad, atomicidad): ver la sección "reservas-svc — decisiones de diseño" del README. Estados: RESERVADA → CONFIRMADA (≥ 1 pago, slide 14) → FINALIZADA/ADEUDADA; CANCELADA solo sin pagos; BLOQUEADA (`POST /reservas/bloqueos`); CERRADA (evento `CERRAR_HOTEL`).
- La colección `habitacion` es réplica de solo lectura por HTTP. Índices creados al arrancar (`MongoIndexInitializer`, `reservas.crear-indices`): `2dsphere` en `hotel.ubicacion` y único en `habitacionId`. `hotel.ubicacion` es `[longitud, latitud]`. Las habitaciones replicadas antes de este arreglo pueden tener las coordenadas invertidas hasta su próximo `ACTUALIZAR_DATOS` (en desarrollo: vaciar la colección `habitacion`).
- Los tests de integración usan Testcontainers (`mongo:7`, cliente 1.21.4 por Docker 29) y se saltean si no hay Docker.
- **Simular la fecha de hoy** (`app.fecha-actual`, vacía = fecha real; variable `APP_FECHA_ACTUAL` en el compose). Sirve para probar en Swagger lo que solo vale después del check-out (`review-cliente`, finalizar):
  1. Con la fecha real: crear la reserva (`POST /reservas`, check-in ≥ hoy) y pagar.
  2. Recrear reservas-svc con una fecha posterior al check-out. Bash: `APP_FECHA_ACTUAL=2027-01-15 docker compose -f docker/docker-compose.yml up -d reservas-svc`. PowerShell: `$env:APP_FECHA_ACTUAL="2027-01-15"; docker compose -f docker/docker-compose.yml up -d reservas-svc`. (También se puede poner `APP_FECHA_ACTUAL=...` en un `.env` dentro de `docker/`.) El log muestra un WARN "FECHA SIMULADA".
  3. Probar `PUT /reservas/{id}/review-cliente` y `PATCH /reservas/{id}/estado` con `FINALIZADA`.
  4. Volver a la fecha real: recrear el servicio sin la variable (`Remove-Item Env:APP_FECHA_ACTUAL` / `unset APP_FECHA_ACTUAL`, y sacarla del `.env` si la pusiste ahí; luego `docker compose -f docker/docker-compose.yml up -d reservas-svc`).
  Con la fecha simulada el reloj queda fijo ese día a las 12:00; no uses la fecha simulada en un entorno real.

## Puertos

| Servicio | Puerto host |
|---|---|
| API Gateway (único punto de entrada + Swagger unificado) | 8080 |
| Eureka (dashboard) | 8761 |
| Grafana / Prometheus (perfil `observability`) | 3000 / 9090 |
| user-svc / reservas-svc / gestion-svc | sin puerto en el host (8080 interno): se llega por el gateway |
| Frontend (nginx de producción) / Vite (perfil `dev`) | 5173 / 5174 |
| RabbitMQ consola | 15672 (admin/admin) |
| phpMyAdmin / pgAdmin / Mongo Express (perfil `tools`) | 6080 / 6081 / 6091 |
| MySQL / PostgreSQL / MongoDB / AMQP (solo con `docker/docker-compose.dev-ports.yml`) | 3306 / 5432 / 27017 / 5672 |
| OTel Collector, Tempo, Loki | no se publican (red de Docker) |

(Dentro de docker los servicios escuchan en 8080. Desde la Etapa 4 no se publican en el host para poder escalarlos; ver "Eureka y escalado".)

## API Gateway (Etapa 3)

`common/dan-spring-gateway`: Spring Cloud Gateway Server **WebMVC** (servlet), Spring Boot 3.5.x + Spring Cloud 2025.0.0. Perfil de compose `gateway`, puerto 8080. Cada ruta = `id` + `uri` (DNS de docker) + `predicate` (`Path`) + `filter` (`StripPrefix=1`), en `application.properties`. Los URIs se pueden pisar con `USER_SVC_URL`, `GESTION_SVC_URL`, `RESERVAS_SVC_URL` (útil para correr el gateway fuera de docker).

| Prefijo | Destino | Ejemplo por el gateway → lo que recibe el servicio |
|---|---|---|
| `/users/**` | user-svc | `/users/huespedes` → `/huespedes` |
| `/gestion/**` | gestion-svc | `/gestion/hoteles` → `/hoteles` |
| `/reservas/**` | reservas-svc | `/reservas/habitaciones/disponibles` → `/habitaciones/disponibles` |

Como los controllers no se renombraron, `UserController` (`/users`) y `ReservaController` (`/reservas`) quedan con el segmento repetido: `/users/users/...` y `/reservas/reservas/...`.

| Servicio | Endpoint (por el gateway) |
|---|---|
| user-svc | `GET /users/huespedes`, `GET /users/huespedes/{id}`, `PUT/DELETE /users/huespedes/{id}`, `GET /users/huespedes/buscar`, `GET/POST /users/huespedes/{id}/tarjetas` |
| user-svc | **`POST /users/users/huesped`** (alta de huésped; `POST /users/huespedes` no existe; responde **201 con el `HuespedResponse` creado** y header `Location: /huespedes/{id}`), `POST /users/users/propietario`, `GET /users/users`, `GET /users/users/dni/{dni}`, `GET /users/users/buscar-dni` |
| user-svc | `/users/propietarios`, `/users/tarjetas`, `/users/bancos`, **`POST /users/auth/login`** (usuario y contraseña → `{userId,email,nombre,givenName,familyName}`; 401 genérico "Usuario o contraseña incorrectos"), `POST /users/auth/google` (503 si no hay `GOOGLE_CLIENT_ID`) |
| gestion-svc | `GET /gestion/hoteles`, `POST /gestion/hoteles/registrar`, `GET /gestion/hoteles/consultar`, `/gestion/hoteles/{id}` |
| gestion-svc | `/gestion/habitaciones`, `/gestion/tarifas` (solo POST/DELETE), `/gestion/tipos-habitacion` |
| reservas-svc | `POST /reservas/reservas`, `GET /reservas/reservas?idHuesped=`, `GET /reservas/reservas/{id}`, `POST /reservas/reservas/{id}/pagos`, `PATCH /reservas/reservas/{id}/estado`, `PUT /reservas/reservas/{id}/review-cliente`, `PUT /reservas/reservas/{id}/review-dueno`, `POST /reservas/reservas/bloqueos` |
| reservas-svc | `GET /reservas/habitaciones/disponibles` (filtros opcionales: `checkIn`+`checkOut`, `cantidadHuespedes`, `precioMin`, `precioMax`, `estrellas`, `amenities`, `lat`+`lng`+`radioKm`, **`hotelId`**), `GET /reservas/habitaciones/{habitacionId}` (solo GET) |

- **CORS**: se configura **solo en el gateway** (`CorsConfig`, propiedad `gateway.cors.allowed-origins` / env `GATEWAY_CORS_ORIGINS`, por defecto `http://localhost:5173,http://localhost:5174`). Los servicios no deben usar `@CrossOrigin`: el gateway reenvía el header `Origin` y se rechazaría (403) o se duplicarían los headers. El frontend llama al gateway (`VITE_API_URL`, por defecto `http://localhost:8080`). El login con Google necesita `http://localhost:5173` como origen autorizado en Google Cloud Console.
- **Swagger unificado** (Etapa 4): `http://localhost:8080/swagger-ui.html`, con un desplegable por servicio. El gateway pide cada documento OpenAPI por sus propias rutas (`/users/v3/api-docs`, `/gestion/v3/api-docs`, `/reservas/v3/api-docs`; propiedades `springdoc.swagger-ui.urls[n]`) y cada servicio declara en su OpenAPI el servidor `/users`, `/gestion` o `/reservas` (`OpenApiConfig`, propiedad `openapi.server-url`; vacía = sin servidor, para correr un servicio suelto) para que "Try it out" pase por el gateway. Los puertos 8081-8083 ya no se publican.
- **El gateway no enruta el actuator**: cualquier `/<servicio>/actuator/**` (por ejemplo `POST /users/actuator/shutdown`) responde 404 sin llegar al servicio (`ActuatorBlockFilter`, test `GatewayActuatorBlockTest`). Ver "Observabilidad".
- Postman: `common/dan-spring-gateway/gateway.postman_collection.json` (variable `gatewayUrl`).
- Referencia de la cátedra: `ETAPA03.md`, `PRACTICA_03.pdf` (rama `catedra/feature/tp03`; no se hace merge porque pisaría la Etapa 2). También tienen un perfil `reactivo` (WebFlux) que acá no se usa.

## Eureka y escalado (Etapa 4)

- **Eureka Server** (`common/dan-eureka-server`, 8761, dashboard `http://localhost:8761`). Los 4 clientes (user, gestion, reservas y el gateway) se registran con el **perfil de Spring `eureka`** (`application-eureka.properties`, que el compose activa con `SPRING_PROFILES_ACTIVE=eureka`). Sin ese perfil `eureka.client.enabled=false`: los tests y correr un servicio desde el IDE no necesitan Eureka. Con el perfil, el gateway enruta con `lb://user-svc|gestion-svc|reservas-svc` (+ `StripPrefix=1`); como Spring Boot enlaza una lista indexada desde una sola fuente, el perfil repite las 3 rutas completas.
- `eureka.server.enable-self-preservation=false` y expulsión cada 5 s son valores **solo de desarrollo** (para ver rápido que una instancia caída desaparece).
- **`GET /info`** en cada servicio: `{servicio, instancia, ip, hora}`; `instancia` es el `HOSTNAME` del contenedor. Por el gateway: `/users/info`, `/gestion/info`, `/reservas/info`.
- **Escalar**: `docker compose -f docker/docker-compose.yml up -d --scale gestion-svc=3` (los servicios no tienen `container_name` ni puertos fijos; `instance-id` = `<servicio>:<HOSTNAME>`). Llamar varias veces a `http://localhost:8080/gestion/info` y ver que cambian `instancia` e `ip`.
- Ejecutar un servicio **fuera de docker** (IDE): sin perfil `eureka`; para que reservas-svc llegue a los otros definir `clients.user-svc.url=http://localhost:8081` y `clients.gestion-svc.url=http://localhost:8083` (los puertos que uno elija al correrlos).

### Qué pasa al escalar
- **Tarea programada**: con 3 réplicas de gestion-svc, `TarifaPrecioScheduler` se ejecuta **3 veces** a las 00:05 (cada réplica tiene su propio `@Scheduled`) y también 3 veces al arrancar (`ApplicationReadyEvent` en cada una), publicando `ACTUALIZAR_PRECIO` repetido. Hoy es inofensivo porque el evento es **idempotente** (reservas-svc hace `updateMulti` por tipo de habitación con el mismo precio), pero es trabajo y mensajes de más, y sería un bug real si la tarea hiciera algo no idempotente (cobrar, enviar mails). En sistemas reales se resuelve con un lock distribuido (**ShedLock**: una tabla/clave en la base; solo la réplica que toma el lock ejecuta esa corrida), una elección de líder (Kubernetes lease, ZooKeeper, Spring Integration), sacando la tarea a un único proceso (CronJob, instancia "scheduler" dedicada) o haciendo la tarea idempotente y deduplicando. **No está implementado**: queda documentado como limitación del escalado.
- **RabbitMQ**: si reservas-svc corre con varias réplicas, todas escuchan la misma cola `habitacion.topic` (patrón **competing consumers**): el broker reparte los mensajes entre los consumidores y **cada mensaje lo procesa una sola réplica** (si una cae sin confirmar, se reentrega a otra). Funciona bien acá porque todas comparten la **misma base MongoDB**: da igual cuál procese el evento, el resultado queda visible para todas (y `CREAR`/`CERRAR_HOTEL` son idempotentes). Matices: se pierde el orden global entre mensajes (dos eventos de la misma habitación podrían procesarse en paralelo en réplicas distintas) y, si cada réplica necesitara recibir **todos** los mensajes (por ejemplo para una caché propia en memoria), haría falta una cola por réplica (fan-out), no una compartida.

## Comunicación sincrónica entre servicios (OpenFeign) — reservas-svc

`POST /reservas` valida contra las fuentes de verdad **antes** de tomar el cupo de la habitación:
1. `UserServiceClient` → `GET /huespedes/{id}` de user-svc: si no existe → **404** "No existe el huésped N"; se guardan `nombreApellido` y `email` en la reserva (visibles como `nombreHuesped`/`emailHuesped`).
2. `GestionServiceClient` → `GET /habitaciones/{id}/disponible` de gestion-svc: `false` → **409**.
- **Por qué además de la réplica local**: la réplica de habitaciones es eventualmente consistente (RabbitMQ); si un `CERRAR_HOTEL` aún no llegó, reservas ofrecería una habitación cerrada. Preguntar a la fuente de verdad cierra esa ventana en el momento de reservar. **Costo**: acoplamiento temporal (si gestion cae no se puede reservar), una llamada de red más por reserva y la consulta no es atómica con `tomarCupo`. Las lecturas (búsqueda, GET) siguen usando solo la réplica.
- **Fallos → 503, nunca 500**: `ServiciosRemotos` traduce cualquier fallo (no responde, timeout, 5xx, sin instancias, circuito abierto) a `ServicioNoDisponibleException` → **503** con mensaje claro; un 404 remoto es un 404 de negocio. Timeouts Feign: conexión 2 s, lectura 3 s (`spring.cloud.openfeign.client.config.default.*`).
- **Circuit breaker (Resilience4j)**: `spring.cloud.openfeign.circuitbreaker.enabled=true`; se abre si falla ≥ 50 % de las últimas 10 llamadas (mínimo 5), 10 s abierto y luego half-open con 2 llamadas de prueba. Con el circuito abierto se responde 503 al instante sin llamar al servicio. Los 404 no cuentan como fallo.
- Tests: `ReservaServiceTest` (clients mockeados), `UserServiceClientIntegrationTest` / `GestionServiceClientIntegrationTest` / `CircuitBreakerIntegrationTest` (WireMock: éxito, 404, 500, conexión cortada, timeout, servicio caído, apertura y recuperación del circuito).

## Observabilidad (Etapa 5)

Profile `observability` del compose raíz: **OpenTelemetry Collector + Tempo + Loki + Prometheus + Grafana** (imágenes con versión fijada, volúmenes `*_data` para no perder datos). Los servicios funcionan igual sin él.

```
métricas  (PULL):  Prometheus ──scrape cada 15 s──> <ip de cada réplica>:9091/actuator/prometheus
                   (instancias descubiertas en Eureka: eureka_sd_configs)
trazas    (PUSH):  servicio ──OTLP/HTTP:4318──> otel-collector ──OTLP/gRPC:4317──> Tempo
logs      (PUSH):  servicio ──OTLP/HTTP:4318──> otel-collector ──OTLP/HTTP /otlp──> Loki
Grafana (:3000, admin/admin) lee de Prometheus, Loki y Tempo (datasources provisionados con uid fijo)
```

- **Cómo levantarlo**: `docker compose -f docker/docker-compose.yml --profile observability up -d --build` (la exportación OTLP de los servicios está siempre encendida: sin el perfil no hay collector y no pasa nada). UI: Grafana `http://localhost:3000` (dashboard "DAN — Servicios"), Prometheus `http://localhost:9090`. Loki y Tempo no se publican en el host (se consultan desde Grafana). Para generar datos: `.\scripts\generar-trafico.ps1` (PowerShell).
- **Qué se corrigió respecto de la implementación de la cátedra** (`ETAPA05-OBSERVABILIDAD.md`, solo referencia): (1) las propiedades `otel.*` son del agente Java y Spring Boot no las lee: acá se usa `management.tracing.*` y `management.otlp.tracing|logging.*` de Boot 3.5; (2) el collector enviaba las trazas a Tempo:3200 (API de consulta): el receptor OTLP es 4317; (3) el exporter `loki` del collector-contrib fue removido: Loki recibe OTLP nativo (`otlphttp` a `/otlp`, esquema v13 + tsdb + metadatos estructurados) y **Promtail ya no hace falta** (los logs salen por OTLP desde un appender de logback; funciona en Docker Desktop/Windows y los logs llevan `trace_id`/`span_id`); (4) Prometheus usa `eureka_sd_configs`, no targets fijos: las réplicas de `--scale` aparecen solas; (5) imágenes fijadas, no `latest`.
- **Config Spring** (bloque "Observabilidad" de cada `application.properties`): `management.otlp.tracing.endpoint` / `management.otlp.logging.endpoint` (`OTEL_COLLECTOR_URL`, por defecto `http://otel-collector:4318`), `management.tracing.sampling.probability` (`TRACING_SAMPLING`, 1.0 en desarrollo; bajarlo en producción). La exportación está **apagada por defecto** (tests, IDE); el compose la enciende con `MANAGEMENT_OTLP_*_EXPORT_ENABLED=${OTEL_EXPORT_ENABLED:-true}`. Sin collector la app arranca y responde igual y no genera errores en el log; `OTEL_EXPORT_ENABLED=false` en `.env` la apaga del todo. Los logs usan `logback-spring.xml` con `OpenTelemetryAppender` (instalado por `ObservabilityConfig`).
- **Versiones de OpenTelemetry**: Spring Boot 3.5.16 gestiona el SDK 1.49.0; el appender se fija en `opentelemetry-logback-appender-1.0:2.15.0-alpha` (propiedad `opentelemetry-instrumentation.version` del pom raíz), la línea de instrumentación que usa ese SDK. La 2.16.0-alpha trae el SDK 1.50.0 y mezclaría versiones. Comprobar con `./mvnw dependency:tree -Dincludes=io.opentelemetry`.
- **Trazas de punta a punta** (propagación W3C `traceparent`): `POST /reservas/reservas` por el gateway = una traza con spans del gateway, reservas-svc, el circuit breaker, las llamadas Feign (a user-svc y gestion-svc, con sus spans de servidor) y los comandos de MongoDB (`MongoObservationCommandListener` registrado en `ObservabilityConfig` de reservas-svc: Boot 3.5 no lo autoconfigura). `POST /gestion/habitaciones` publica en RabbitMQ con observación (`RabbitTemplate.setObservationEnabled(true)`: gestion-svc define su propio `RabbitTemplate`, por lo que la propiedad de Boot no aplica) y reservas-svc consume con `spring.rabbitmq.listener.simple.observation-enabled=true`: el consumo y sus accesos a Mongo continúan **la misma traza**. JPA/JDBC no están instrumentados (no hay spans de SQL).
- **Resilience4j**: se desactiva el bulkhead de Spring Cloud (`spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled=false`). `resilience4j-micrometer` (necesario para `resilience4j_circuitbreaker_state`) arrastra `resilience4j-bulkhead` y Spring Cloud pasaría a ejecutar cada llamada Feign en OTRO hilo: envolvía las excepciones (el `ignore-exceptions` de los 404 dejaba de funcionar y el circuito se abría con 404) y cortaba el contexto de la traza. Lo detectó `CircuitBreakerIntegrationTest`.
- **Ruido filtrado**: `ObservabilityConfig` descarta como observaciones los pedidos a `/actuator/**` (scrapes de Prometheus) y los heartbeats al servidor Eureka; si no, llenarían Tempo.
- **Seguridad del actuator**: los 4 servicios exponen solo `health`, `info` y `prometheus` (`management.endpoints.web.exposure.include`), `shutdown` deshabilitado (`management.endpoint.shutdown.access=none`), `health.show-details=never`. En Docker el actuator escucha en un **puerto de gestión aparte (9091, `MANAGEMENT_SERVER_PORT`)** que no se publica en el host ni enruta el gateway: Prometheus lo lee por la red de Docker. El gateway además corta `/<servicio>/actuator/**` con 404. Tests: `ActuatorExposureTest` (4 módulos) y `GatewayActuatorBlockTest`. Eureka Server (8761) mantiene `health,info` y no se instrumenta.
- **Eureka con el puerto 9091**: cada instancia registra `homePageUrl` en `:8080` y `statusPageUrl`/`healthCheckUrl` en `http://<ip>:9091/actuator/{info,health}` (verificable en `http://localhost:8761/eureka/apps`). Las IPs de contenedor no son alcanzables desde el navegador del host (tampoco antes, con el 8080): el link del dashboard muestra la URL pero se abre solo desde dentro de la red de Docker. El único healthcheck de actuator en el compose es el de Eureka sobre su propio puerto.
- **Prometheus**: job `dan-servicios` (`job` = nombre de la app en minúsculas, `instance` = `<servicio>:<id del contenedor>`, `__address__` = `<ip>:9091`); jobs estáticos solo para `prometheus` y `otel-collector:8888`. El puerto 9091 está fijo en `docker/config/prometheus/prometheus.yml`.
- **Dashboard** (`docker/config/grafana/dashboards/dan-servicios.json`, variables `job`/`instance`): instancias vivas, pedidos/seg, latencia p50/p95/media por servicio, errores 5xx por servicio y URI, % 5xx, heap/no-heap/GC/threads/CPU, y **estado del circuit breaker** (cerrado/semiabierto/abierto, tasa de fallos, llamadas rechazadas, llamadas por resultado). Saltos: Loki → Tempo (derived field `trace_id`) y Tempo → Loki (`tracesToLogsV2`, filtra `{service_name=…} | trace_id=…`).
- **Memoria** (Docker Desktop tiene 7,7 GiB en esta máquina): las JVM llevan tope (`mem_limit` + `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65|60 -XX:+UseSerialGC -Xss512k`): user/gestion/reservas 512 MiB, gateway y Eureka 384 MiB; collector 256 MiB, Tempo/Loki/Grafana 384 MiB, Prometheus 512 MiB. Medido con todo arriba (18 contenedores, 2 réplicas de gestion-svc, pgAdmin/phpMyAdmin/Mongo Express incluidos): **≈ 3,9 GB**, sin OOM ni reinicios (JVM ~300 MiB c/u, MySQL 431, MongoDB 286, pgAdmin 295, RabbitMQ 209, Loki 190, Tempo 172, Grafana 150, Prometheus 141, collector 81). **Hacen falta ≥ 6 GB asignados a Docker Desktop** (≥ 7 GB si se escala a 3+ réplicas; cada réplica suma ~0,3–0,5 GB). pgAdmin, phpMyAdmin y Mongo Express (profile `infra`, ~0,4 GB) se pueden dejar afuera si falta memoria.
- **Qué pasa al escalar**: ver "Qué pasa al escalar" (Etapa 4); además, cada réplica aparece como una serie distinta en Prometheus (`instance`) y todas envían sus trazas/logs al mismo collector.

## Frontend (ticket 6000)

SPA en `frontend/` (React 19 + TypeScript + Vite + Tailwind/SCSS, `react-router-dom`; clases `rarn-*`, componentes en `ui/` y `layout/`). Todo pasa por el gateway.

- **Rutas**: `/` (Hero + `SearchBar`, mapa, hoteles destacados), `/buscar` (resultados: filtros precio/estrellas/amenities enviados al backend, orden en el cliente, `?hotelId=` para un hotel), `/habitaciones/:id` (detalle + reservar), `/login`, `/registro`, `/mis-reservas` (protegida por `RutaProtegida`).
- **Capa de API** (`src/api/`): `client.ts` (fetch; `ApiError{status,message}` con el `message` del backend; `mensajeAmigable`: 503 → "El servicio no está disponible, probá en unos segundos", red caída → status 0), `types.ts` (DTOs reales), `auth|hoteles|habitaciones|reservas.ts`. Hook `useAsync` (loading/error/reload con `AbortSignal`).
- **Datos**: destacados = `GET /gestion/hoteles` sin cerrados + precio "desde" = mínimo `precioNoche` de `GET /reservas/habitaciones/disponibles` agrupado por `hotel.id` (si falla, igual se muestran sin precio); imagen = `hotel{1,2,3}.webp` por `id % 3` (`util/hotelImage.ts`). "Ver detalles" → `/buscar?hotelId=<id>`. Ciudades con coordenadas en `util/ciudades.ts` (Santa Fe, Rosario, Córdoba, Buenos Aires, Mendoza) o "Cerca de mi ubicación" (`navigator.geolocation`).
- **Mis reservas**: los botones se habilitan con `util/reservaReglas.ts` (espejo de `ReservaPolicy`: pagar si no está CANCELADA/FINALIZADA/BLOQUEADA/CERRADA y hay saldo; cancelar solo RESERVADA/BLOQUEADA sin pagos; opinar desde el día del check-out y una vez). Es una ayuda de UI: **decide el backend** y su 409 se muestra tal cual.
- **Sesión**: `context/auth.ts` (contexto + `useAuth`) y `AuthContext.tsx` (provider; **única** escritora de `localStorage['user']`). Login con usuario/contraseña (`POST /users/auth/login`); registro de huésped (`POST /users/users/huesped`, exige una tarjeta principal; bancos de `GET /users/bancos`; usa el id de la respuesta, ya no se busca por DNI). `?redirect=` solo acepta rutas internas (`util/redirect.ts`).
- **Google es opcional**: `GoogleLoginButton` se renderiza (y carga el script de Google, inyectado dinámicamente) **solo si `VITE_GOOGLE_CLIENT_ID` está definido**. user-svc ya no exige `GOOGLE_CLIENT_ID` para arrancar (sin él, `POST /users/auth/google` → 503).
- **Bancos iniciales**: el compose no monta `docker/config/mysql/initdb`, así que `BancoDataInitializer` (user-svc, `ApplicationRunner`) inserta una lista de bancos argentinos **solo si la tabla `bancos` está vacía** (idempotente).
- **`VITE_*` se fijan al compilar**: `VITE_API_URL` (dev: `http://localhost:8080`; prod: `/api`) y `VITE_GOOGLE_CLIENT_ID`. En dev se leen al iniciar Vite; en prod entran como build args de `docker/frontend.Dockerfile` (`PROD_VITE_API_URL`, `VITE_GOOGLE_CLIENT_ID`): para cambiarlas hay que reconstruir la imagen (`docker compose -f docker/docker-compose.yml build frontend`).
- **Dev vs prod**:
  - `frontend-dev` (perfil `dev`, Vite en **5174**): el navegador llama **directo al gateway** (`http://localhost:8080`), por eso el gateway necesita CORS para `:5174` (ya está permitido). `package-lock.json` ya no se borra (el comando usa `npm install`, que respeta el lock; antes un `rm -f package-lock.json` sobre el bind mount lo eliminaba del host en cada arranque); `node_modules` vive en el volumen `frontend_node_modules`.
  - `frontend` (nginx sin root, 5173; es el que levanta el compose por defecto): sirve el build y hace de **proxy**: `location /api/` reenvía a `http://gateway:8080` (quita `/api`; `resolver 127.0.0.11` + variable para que nginx arranque aunque el gateway no esté). El front y la API quedan en el **mismo origen** → no hace falta CORS.
- **Calidad**: `npm run typecheck`, `npm run lint`, `npm test` (Vitest + Testing Library, jsdom), `npm run build` (corre `tsc` antes de Vite). Tests en `*.test.ts(x)` junto al código; `src/test/` tiene el `setup` y `renderConApp`.
- **Bases en el host (solo desarrollo)**: ya no se publican por defecto; `docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev-ports.yml up -d` publica MySQL 3306, PostgreSQL 5432, MongoDB 27017 y AMQP 5672. En Windows el 3306 puede estar reservado por el sistema ("Intento de acceso a un socket no permitido", `netsh interface ipv4 show excludedportrange protocol=tcp`): `MYSQL_HOST_PORT=13306`.

### Seguridad del login (qué hay y qué falta)
- **Hay**: contraseñas con BCrypt (`spring-security-crypto`), `POST /auth/login` con error genérico (no revela si falló el usuario o la clave; compara contra un hash ficticio si el usuario no existe), cuentas de Google sin contraseña no pueden entrar por clave, token de Google verificado por firma en el servidor, tarjetas enmascaradas y sin CVC en las respuestas, actuator fuera del gateway, CORS acotado, redirecciones del login solo internas.
- **NO hay (mejora pendiente, no implementada)**: el login solo devuelve la identidad `{userId,email,nombre}`, **sin token ni sesión de servidor**. `localStorage['user']` es declarativo: la API no verifica quién llama, así que cualquiera que llegue al gateway puede pedir o pagar reservas de cualquier `idHuesped`, modificar/borrar huéspedes, etc. Un XSS podría además leer lo guardado.
- **Qué haría falta**: (1) emitir un **JWT firmado** en `/auth/login` y `/auth/google` (user-svc) con `sub`=userId, rol y expiración corta (+ refresh); (2) **validarlo en el gateway** (filtro que verifique firma/expiración y responda 401, y propague `X-User-Id`/roles a los servicios; los servicios en la red interna confían solo en el gateway); (3) **autorización por dueño** en los servicios (p. ej. reservas-svc compara el `userId` del token con `idHuesped`; solo el dueño ve/paga/cancela) y por rol (huésped / propietario / admin); (4) guardar el token de forma segura (cookie `HttpOnly`+`Secure`+`SameSite` + protección CSRF, o memoria + refresh en cookie), HTTPS de extremo a extremo; (5) límite de intentos / bloqueo de cuenta en el login y auditoría.

## Docker de entrega (ticket 7000)

Consigna: el TP en la raíz con una carpeta `docker/` (yaml + Dockerfile) para levantarlo con Docker. Quien corrige solo tiene Docker (sin JDK, Maven, Node ni `.env`).

- **Un comando**: `docker compose -f docker/docker-compose.yml up -d --build` (o `docker compose up -d --build` desde la raíz: el `docker-compose.yml` de la raíz solo hace `include` del de `docker/`; se verificó que las rutas relativas se resuelven y que un `.env` de la raíz sigue interpolándose). Proyecto `name: dan-reservas`.
- **`docker/backend.Dockerfile`** (contexto = raíz del repo): etapa `build` con `maven:3.9-eclipse-temurin-21` que compila el reactor **una sola vez** (`mvn -Dmaven.test.skip=true package`, con `--mount=type=cache` de `~/.m2`); etapa `runtime` con `eclipse-temurin:21-jre-alpine`, usuario no-root `app` (uid 10001) y un **target por servicio**: `user-svc`, `gestion-svc`, `reservas-svc`, `gateway`, `eureka`, cada uno con su `HEALTHCHECK` (`wget` a `:9091/actuator/health`; Eureka a `:8761`). El `ENV MANAGEMENT_SERVER_PORT=9091` va en cada target, **no** en la base (Eureka usa su propio puerto). `.dockerignore` de la raíz excluye `target`, `node_modules`, `.env`, `frontend`, `docker`, `docs`.
- **`docker/frontend.Dockerfile`** (contexto = `frontend/`): Node 20 (`npm ci` con cache de npm) → `nginxinc/nginx-unprivileged` (escucha en 8080, publicado como 5173; healthcheck a `127.0.0.1`, no `localhost`, que resuelve a IPv6).
- **Perfiles**: sin perfil = app completa (3 bases, RabbitMQ, Eureka, 3 servicios, gateway, front). `observability`, `tools`, `dev` (`frontend-dev`, Vite en 5174). Puertos publicados: 5173, 8080, 8761, 15672 (+3000 y 9090 con observability). La exportación OTLP está **siempre encendida** (decisión: sin collector no pasa nada; así `--profile observability` alcanza para ver trazas).
- **Configs** en `docker/config/` (antes `infra/`, movidos con `git mv`). RabbitMQ: `rabbitmq.conf` + `definitions.json` (`load_definitions`). **Ojo**: al importar definiciones RabbitMQ **no crea** el usuario de `RABBITMQ_DEFAULT_USER/PASS`: por eso `definitions.json` incluye el usuario `admin` (contraseña `admin`, hash SHA-256 con sal).
- **Datos de demo** (`DEMO_DATOS_HABILITADO`, `true` en Docker, `false` por defecto en `application.properties`/tests): `DemoDataInitializer` (gestion-svc; hoteles de 6 ciudades con CUIT válido calculado, amenities, tarifas por tipo y habitaciones, **siempre por los services** para que se publiquen los eventos de RabbitMQ; idempotente por CUIT / tipo con tarifas / hotel+número) y `DemoUsuarioInitializer` (user-svc; `demo` / `demo1234`, `@Order(1)` tras `BancoDataInitializer`). gestion-svc declara `dan.exchange`, `habitacion.topic` y el binding (`RabbitMQConfig.habitacionesTopologia`) para no perder eventos si reservas-svc arranca después. Limitación: borrar solo MongoDB no re-replica (usar `down -v`).
- **Imágenes del front**: `frontend/src/assets/images/hotel1–6.webp` (< 300 KB c/u; 4–6 derivadas de las originales con recortes/espejado/color). `util/hotelImage.ts` usa `id % 6`; el `Hero` usa las 3 primeras.
- **Postman**: una sola colección, `docs/postman/DAN - API Gateway.postman_collection.json` (≈ 86 requests por el gateway). Las carpetas `.postman/` y `postman/` de la raíz las crea la app de escritorio de Postman y están excluidas en `.git/info/exclude` (reglas ancladas a la raíz: `/postman/`, para no ignorar `docs/postman/`).
- **CI** (`.github/workflows/ci.yml`): `test-backend`, `frontend` (typecheck, lint, tests, build), `docker-build` (`compose config` con todos los perfiles + `bake` de las 6 imágenes con cache gha) y `dependency-review`.
- **Medido** (esta máquina, Docker Desktop, imágenes base ya descargadas): primer build de las 6 imágenes **136 s**; reconstruir sin cambios 8 s; con 1 línea de Java 51 s; con 1 línea del front 40 s; `up` completo hasta *healthy* ~66 s tras el build.

## Datos sensibles de tarjetas (user-svc)
`TarjetaCreditoResponse` **no incluye el CVC** y devuelve el `numero` enmascarado (`**** **** **** 0004`, `TarjetaMascara`, aplicado en `TarjetaCreditoMapper.toResponse`). El request sigue aceptando `cvc`.

## Convención de commits

`feat(XXXX): descripción` y `fix(XXXX): descripción`, donde `XXXX` es el número de ticket/issue (ej. `feat(1005): ...`). Otros prefijos usados: `docs`, `build`, `test`. **Siempre correr `./mvnw spotless:apply` antes de commitear.**

Ramas de trabajo actuales: `development` (base), `feature/completar-tp` (preparación/finalización del TP).

## Reglas de negocio

### ETAPA 01 — user-svc
- Usuarios de dos tipos: **Huésped** y **Propietario**. Datos comunes: nombre, email, teléfono (+ DNI, agregado al modelo/BD).
- **Huésped**: al registrarse se carga una tarjeta de crédito (número, titular, código de seguridad, vencimiento, banco emisor, booleano *principal*). Puede tener varias tarjetas pero **solo una principal**.
- **Propietario**: cuenta bancaria + id de hotel (null en etapa 1; se asigna con el servicio de gestión).
- Gestión de usuarios: de los huéspedes se pueden actualizar datos de usuario y fecha de nacimiento; buscar usuarios por **nombre o DNI**; **borrar huésped** borra en cascada sus tarjetas; **no se pueden borrar propietarios**.
- Tarjetas: agregar a un huésped (si es principal, desmarcar la anterior); eliminar solo si **no es la principal** (si lo es, error); cambiar la principal.
- Bancos: CRUD completo.
- Endpoints REST, JPA o JDBC, Swagger opcional.

### ETAPA 02 — gestion-svc (hoteles, habitaciones, tarifas)
**Hoteles**
- Alta con todos obligatorios: nombre, CUIT, dirección, coordenadas (lat/lng); validar con Bean Validation.
- Solo se pueden actualizar **categoría, teléfono y correo**; el resto no cambia.
- No se dan de baja: se marca **Cerrado** (atributo o fecha de cierre) y automáticamente todas sus habitaciones quedan no disponibles. Se envía mensaje a reservas-svc y allí se crea una reserva tipo **CERRADA** para todas las habitaciones (inicio hoy, fin null).
- PUT para agregar una amenity o lista de amenities; DELETE para quitar una amenity.
- GET para consultar por nombre, categoría, amenities, dirección, etc.

**Habitaciones** (datos: número, piso, tipo de habitación, disponible)
- Alta/modificación/baja; cada operación **notifica por RabbitMQ (JMS) a reservas-svc**.
- Validación: número y piso no null y **> 0**.
- Consulta por cantidad de huéspedes y/o tipo de habitación y/o precio mínimo/máximo.
- GET que retorna la **tarifa vigente** de una habitación.
- Tipos de habitación: nombre, descripción, capacidad.

**Tarifas** (nombre, fecha inicio, fecha fin, precio, idTipoHabitacion)
- Solo POST y DELETE (sin modificación).
- Solo se puede eliminar si **existe otra tarifa** para ese tipo de habitación. Si se elimina la vigente (según fechas), la **anterior pasa a vigente**; si no es vigente se borra directamente.
- Al agregar una tarifa a un tipo que ya tiene y la nueva no trae fechas: la actual termina **el día anterior a hoy** y se crea la nueva con inicio hoy y fin null.
- **Tarifa especial/promocional** (con inicio y fin): la actual termina el día anterior al inicio de la promo; se crea la promo; se crea otra tarifa que empieza el día posterior a la promo con fin null.

### ETAPA 02 — reservas-svc (MongoDB)
- Mantiene una colección local de habitaciones actualizada **solo por mensajes** (creación, actualización de datos, de tarifas, eliminación). Habitaciones/hoteles **no** se crean/modifican/borran por HTTP: la fuente de verdad es gestion-svc.
- Cada habitación guarda la lista de fechas/reservas para calcular disponibilidad.
- Reserva: habitación + check-in + check-out. Estados:
  - se crea **RESERVADA**;
  - con **al menos un pago** → **CONFIRMADA**;
  - mientras no tenga pago puede ser **CANCELADA** (se elimina de las reservas de la habitación);
  - al marcar **FINALIZADA** es obligatorio que el dueño deje un review y que el pago esté completo; si no, queda **ADEUDADA**;
  - **BLOQUEADA / CERRADA**: la habitación no se ofrece.
  - Tras el checkout se puede agregar rating.
- Operaciones: alta, consulta por usuario, actualización de estado, gestión del pago.
- **Búsqueda de habitaciones disponibles** por: fecha ingreso/salida, cantidad de huéspedes, precio mín/máx, estrellas del hotel, amenities, y distancia (m/km) a una lat/lng de referencia (**índice Geo2D** de MongoDB).
- Una habitación está disponible si **no** tiene reservas con `ingreso <= fechaSalidaPedida` y `salida >= fechaIngresoPedida` (solapamiento).

## Etapas posteriores (cátedra, aún no integradas)
Referencia en `catedra/feature/tp03` (Spring Cloud Gateway, ya integrado), `tp04` (Eureka + OpenFeign, ya integrado; docs en `ETAPA04.md` y `ETAPA04-IMPL.md`) y `tp05` (observabilidad con OpenTelemetry/Loki/Tempo/Prometheus/Grafana, ya integrada con correcciones: ver "Observabilidad (Etapa 5)"). Docs: ETAPA03.md, ETAPA04.md, ETAPA04-IMPL.md, ETAPA05-OBSERVABILIDAD.md (esta última describe la implementación de la cátedra, con los errores que acá se corrigieron).
