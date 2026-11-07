# Guía de desarrollo

Todo lo que no hace falta para *usar* el sistema (eso está en el [README](../README.md)) pero sí para *trabajar* en él.

## Requisitos de desarrollo

- **Java 21** (JDK), **Node 20+** y **Docker Desktop**. Para usar el sistema solo hace falta Docker.
- En esta máquina de desarrollo el `java` del PATH puede ser el 1.8: hay que fijar `JAVA_HOME` por sesión.
  PowerShell: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"`.

## Compilar y probar

```bash
./mvnw clean install                 # compila todo y corre los tests de los 5 módulos
./mvnw -pl services/gestion-svc test # tests de un servicio
./mvnw spotless:apply                # formatear (obligatorio antes de commitear)
```

- Los tests de integración de reservas-svc usan Testcontainers (`mongo:7`, RabbitMQ) y se saltean si no hay Docker.
- **Windows/OneDrive**: a veces `clean` falla con "Failed to delete ...\target\classes\..." por un lock; borrar los `target/` a mano y reintentar.

Frontend (carpeta `frontend/`): `npm ci`, `npm run dev`, `npm run typecheck`, `npm run lint`, `npm test`, `npm run build`.

## Perfiles de Docker Compose

```bash
docker compose -f docker/docker-compose.yml up -d --build                       # app completa (sin perfiles)
docker compose -f docker/docker-compose.yml --profile observability up -d       # + Collector, Tempo, Loki, Prometheus, Grafana
docker compose -f docker/docker-compose.yml --profile tools up -d               # + pgAdmin, phpMyAdmin, Mongo Express
docker compose -f docker/docker-compose.yml --profile dev up -d frontend-dev    # front con Vite en caliente: http://localhost:5174
```

- Las variables tienen valores por defecto (`${VAR:-valor}`); si hace falta cambiar alguna, se puede crear un `.env` en `docker/` (ver `.env.example`) o exportarla en la terminal.
- **Publicar las bases en el host** (MySQL 3306, PostgreSQL 5432, MongoDB 27017, AMQP 5672) para conectarse con un cliente SQL o correr un servicio desde el IDE:
  `docker compose -f docker/docker-compose.yml -f docker/docker-compose.dev-ports.yml up -d`.
  En Windows el 3306 puede estar reservado por el sistema ("Intento de acceso a un socket no permitido"; ver `netsh interface ipv4 show excludedportrange protocol=tcp`): cambiar el puerto con `MYSQL_HOST_PORT=13306`.
- Los scripts de `docker/config/*/initdb` solo se ejecutan con el volumen de la base vacío: si cambia uno, `docker compose down -v`.

## Simular la fecha de hoy

`app.fecha-actual` (variable `APP_FECHA_ACTUAL`) fija el "hoy" de reservas-svc. Sirve para probar lo que solo vale después del check-out (`review-cliente`, finalizar):

1. Con la fecha real: crear la reserva y pagar.
2. Recrear reservas-svc con una fecha posterior al check-out: `APP_FECHA_ACTUAL=2027-01-15 docker compose -f docker/docker-compose.yml up -d reservas-svc` (PowerShell: `$env:APP_FECHA_ACTUAL="2027-01-15"; docker compose ...`). El log muestra un WARN "FECHA SIMULADA".
3. Probar `PUT /reservas/reservas/{id}/review-cliente` y `PATCH /reservas/reservas/{id}/estado`.
4. Volver a la fecha real recreando el servicio sin la variable. No usar la fecha simulada en un entorno real.

## Datos de demo

`DEMO_DATOS_HABILITADO` (por defecto `true` en Docker, `false` en tests/IDE) activa dos `ApplicationRunner` idempotentes:

- **gestion-svc** `DemoDataInitializer`: 6 hoteles (Santa Fe, Rosario, Córdoba, Buenos Aires, Mendoza, Bariloche), sus amenities, una tarifa por tipo de habitación y 4–6 habitaciones por hotel. Todo pasa por los *services* (no por los repositorios), así que cada alta publica su evento de RabbitMQ y reservas-svc replica las habitaciones como con cualquier alta. Cada paso se saltea si el dato ya existe (hotel por CUIT, tarifa si el tipo ya tiene alguna, habitación por hotel+número).
- **user-svc** `DemoUsuarioInitializer`: huésped `demo` / `demo1234` con una tarjeta principal (después de los bancos de `BancoDataInitializer`).
- `DEMO_DATOS_HABILITADO=false docker compose ... up -d` los desactiva.
- gestion-svc declara el exchange `dan.exchange`, la cola `habitacion.topic` y su binding al conectarse a RabbitMQ (y el broker los carga de `docker/config/rabbitmq/definitions.json`): si reservas-svc arranca después, los eventos esperan en la cola.
- Limitación: si se borra solo MongoDB (y no PostgreSQL) las habitaciones no se vuelven a replicar; usar `docker compose down -v`.

## Escalar y observar

- `docker compose -f docker/docker-compose.yml up -d --scale gestion-svc=3` y llamar varias veces a `http://localhost:8080/gestion/info` (cambia `instancia` e `ip`).
- Generar tráfico para ver datos en Grafana: `.\scripts\generar-trafico.ps1` (PowerShell).
- Detalle de la arquitectura de observabilidad, Eureka y Feign en `CLAUDE.md`.

## Postman

Una sola colección: [`docs/postman/DAN - API Gateway.postman_collection.json`](postman/DAN%20-%20API%20Gateway.postman_collection.json) (variable `gatewayUrl`, ≈ 85 requests por el gateway). Empezar por la carpeta "0. Flujo completo".

## Convenciones

- **Tickets**: `[0010]-[T]-[I1] > Título descriptivo`. **Ramas**: `0010-t-i1-titulo-descriptivo`. **Commits**: `feat(0010): ...`, `fix(0010): ...`, también `docs`, `build`, `test`, `chore`. Siempre `./mvnw spotless:apply` antes de commitear.
- Flujo: registrar el tiempo en el ticket → PR hacia `development` (etiqueta `testing`) → describir qué y cómo se probó → otro integrante revisa → merge.
- Herramientas: VSCode, Git/GitHub, Quire.

## Material de la cátedra

- Etapa 01/02: [ETAPA01.md](../ETAPA01.md), [ETAPA02.md](../ETAPA02.md), [PRACTICA_02.pdf](../PRACTICA_02.pdf), [TP_PARTE_02.pdf](../TP_PARTE_02.pdf).
- Etapa 03 (API Gateway): [ETAPA03.md](../ETAPA03.md), [PRACTICA_03.pdf](../PRACTICA_03.pdf).
- Etapa 04 (Eureka + OpenFeign): [ETAPA04.md](../ETAPA04.md), [ETAPA04-IMPL.md](../ETAPA04-IMPL.md) (versión de la cátedra).
- Etapa 05 (Observabilidad): [ETAPA05-OBSERVABILIDAD.md](../ETAPA05-OBSERVABILIDAD.md) (versión de la cátedra, con los errores que acá se corrigieron).

## Login con Google (opcional)

Sin `GOOGLE_CLIENT_ID` user-svc arranca igual, `POST /users/auth/google` responde 503 y el front no muestra el botón; el login con usuario y contraseña funciona siempre. Para habilitarlo definir `GOOGLE_CLIENT_ID` (backend) y `VITE_GOOGLE_CLIENT_ID` (front; se incrusta al **compilar**, hay que reconstruir la imagen del front) con un Client ID real que tenga `http://localhost:5173` como origen autorizado. Detalle del frontend (rutas, capa de API, dev vs prod, `VITE_*`) en [CLAUDE.md](../CLAUDE.md#frontend-ticket-6000).
