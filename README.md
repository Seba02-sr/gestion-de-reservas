# Trabajo Práctico DAN 2025

Aplicación distribuida con microservicios (Spring Boot) y frontend moderno. Monorepo con Docker Compose para levantar infraestructura, backend y frontend.

## Desarrolladores
- Arrua Alejandro
- Nicle Santiago
- Ramella Sebastian

## Cómo levantar el proyecto

1) Requisitos
- Java 21, Docker, Node 20+ (para desarrollo del front)

2) Compilar backend (JARs)
```bash
./mvnw -DskipTests clean package
```

3) Elegir perfiles de ejecución
- Copiar ejemplo: `cp .env.example .env`
- Editar `COMPOSE_PROFILES` según necesidad:
  - Todo (frontend dev + infra + microservicios):
    - `COMPOSE_PROFILES=frontend,infra,user,reservas,gestion`
  - Solo frontend dev: `COMPOSE_PROFILES=frontend`
  - Solo un servicio (con su infra mínima):
    - user-svc: `COMPOSE_PROFILES=frontend,user`
    - reservas-svc: `COMPOSE_PROFILES=frontend,reservas`
    - gestion-svc: `COMPOSE_PROFILES=frontend,gestion`
  - Frontend producción (Nginx):
    - `COMPOSE_PROFILES=prod,infra,user,reservas,gestion`

4) Levantar con Docker Compose
```bash
docker compose up -d --build
```

5) URLs y credenciales
- Frontend dev: `http://localhost:8080`
- Frontend prod: `http://localhost:8080` (perfil `prod`)
- user-svc: `http://localhost:8081`
- reservas-svc: `http://localhost:8082`
- gestion-svc: `http://localhost:8083`

- MySQL: `localhost:3306`
  - Base: `users`
  - Usuario app: `usr_app` / `usrapp`
  - Root: `root` / `rootpwd`
  - phpMyAdmin: `http://localhost:6080`

- PostgreSQL: `localhost:5432`
  - Base: `appdb`
  - Usuario app: `appuser` / `apppwd`
  - pgAdmin: `http://localhost:6081` (login: `admin@admin.com` / `admin`)

- MongoDB: `localhost:27017`
  - Admin: `root` / `rootpwd`
  - Mongo Express: `http://localhost:6091`

- RabbitMQ UI: `http://localhost:15672` (login: `admin` / `admin`)

6) Bajar servicios
```bash
docker compose down
# con volúmenes (borra datos):
docker compose down -v
```

---

<<<<<<< Updated upstream
## reservas-svc — decisiones de diseño

Servicio de reservas sobre MongoDB (`http://localhost:8082`, Swagger en `/swagger-ui/index.html`). La
colección `habitacion` es una **réplica** que se actualiza solo con los mensajes de RabbitMQ de
gestion-svc (la fuente de verdad); por HTTP las habitaciones son de solo lectura.

### Regla de confirmación de pagos (el enunciado se contradice)
- Slide 12: la reserva queda CONFIRMADA "cuando se paga **al menos la mitad**".
- Slide 14: "cuando la reserva tiene **al menos un pago** pasa a estado CONFIRMADA".
- **Se implementó la slide 14** (≥ 1 pago). La regla está aislada en un único método,
  `ReservaPolicy.pagoAlcanzaParaConfirmar`; pasar a "la mitad" es cambiar esa línea
  (`totalPagado ≥ precioTotal / 2`) y su test.

### Estados
`RESERVADA` (al crear) → `CONFIRMADA` (primer pago) → `FINALIZADA` / `ADEUDADA`; `CANCELADA` (solo
sin pagos); `BLOQUEADA` (el dueño bloquea fechas con `POST /reservas/bloqueos`); `CERRADA` (llega
por el evento `CERRAR_HOTEL`). El enunciado también menciona REALIZADA y EFECTUADA (slide 12): no se
implementaron, se usan los estados de la slide 14.
- `PATCH /reservas/{id}/estado` acepta solo `CANCELADA` (desde RESERVADA/BLOQUEADA sin pagos) y
  `FINALIZADA` (desde RESERVADA/CONFIRMADA/ADEUDADA). `FINALIZADA` exige el review del dueño
  (`PUT /review-dueno`) **y** el pago completo; si falta algo la reserva queda `ADEUDADA` (se puede
  reintentar). Desde RESERVADA no hay pagos, así que queda ADEUDADA.
- Todo cambio actualiza la `Reserva` y la copia embebida (`ReservaSimple`) en la habitación.
- `PUT /review-cliente` solo después del check-out (el día del check-out ya vale).

### Disponibilidad
Una habitación está **ocupada** para `[checkIn, checkOut)` si tiene una reserva **no CANCELADA** con
`reserva.checkIn < checkOut pedido` **y** (`reserva.checkOut` es null **o** `reserva.checkOut >
checkIn pedido`). Dos estadías pueden encadenarse el mismo día (sale uno, entra otro) y el `null`
cubre las reservas abiertas (CERRADA). La fórmula de la slide 15 está mal redactada (compara la
entrada con la entrada y usa extremos cerrados); el razonamiento está comentado en
`Disponibilidad.java`, donde conviven la versión Java y la de Mongo (un test las compara).

### Reserva concurrente: operación atómica, no transacción
Dos pedidos simultáneos no pueden quedarse con las mismas fechas. Se resuelve con **un `findAndModify`
sobre el documento de la habitación** cuyo filtro incluye "no hay reservas que se superpongan" y que
hace el `$push` de la reserva en la misma operación: Mongo garantiza atomicidad por documento, así
que solo un pedido modifica el documento. Se eligió sobre una transacción porque el Mongo del
compose es standalone (las transacciones piden replica set) y porque, al estar las reservas
embebidas en la habitación, alcanza con un documento. Orden: primero se toma el cupo y después se
guarda la `Reserva`; si el guardado falla, se libera el cupo. Un corte en el medio deja a lo sumo
fechas bloqueadas de más (seguro), nunca una doble reserva. Hay un test con 10 hilos contra un Mongo
real. Los pagos y el resto de los cambios de estado son lectura-modificación-escritura de una
`Reserva` (no compiten por fechas).

### Consistencia eventual
reservas-svc y gestion-svc tienen bases distintas y se enteran por mensajes: entre el cambio en
gestion-svc y su réplica puede pasar un instante (p. ej. una habitación recién creada tarda en
aparecer). `CREAR` y `CERRAR_HOTEL` son idempotentes (índice único + `upsert`, y condición atómica
"no tiene ya una reserva CERRADA"), así que un mensaje repetido no duplica nada. Las reservas ya
existentes de un hotel que cierra no se cancelan automáticamente.

### Fechas
Los check-in/check-out son días (`yyyy-MM-dd`) y se guardan como la medianoche de ese día en
`America/Argentina/Buenos_Aires` (`app.zone`). "Hoy" sale de un `Clock`; para probar a mano se puede
simular la fecha con `APP_FECHA_ACTUAL` (ver CLAUDE.md).

### Geo
`hotel.ubicacion` es un punto GeoJSON `[longitud, latitud]` (x = longitud, y = latitud) con índice
`2dsphere` creado al arrancar. La búsqueda por cercanía usa `$geoWithin` + `$centerSphere`
(combinable con los demás filtros).

## Guías para el desarrollador y el usuario

### Convención de nombre de ticket

**Formato:**

```
[0010]-[T]-[I1] > Ejemplo de nombre de ticket
```
### Convención de nombre de commit

**Formato:**
```
feat(0010): descripción corta
fix(0010): corrección breve
```
Usa feat para funcionalidades nuevas, fix para correcciones.

### Convención de nombre de rama
**Formato:**
```
0010-t-i1-ejemplo-de-nombre-de-ticket
```




## Guía para completar una tarea
**1- Al finalizar la tarea:**

- Registrar el tiempo estimado que llevó realizarla en el campo correspondiente del ticket.
- Crear un Pull Request (PR) desde la rama de trabajo hacia development (link propuesto en consola al realizar un push).
- Asignar la etiqueta testing a la tarea correspondiente.
- Dejar comentario en el ticket con el link al PR.

**2- En el Pull Request:**

- Asegurarse de que la funcionalidad fue testeada manualmente.
- Incluir una descripción breve indicando qué funcionalidades se probaron y cómo.

**3- Revisión y merge:**

- Otro desarrollador debe revisar el código y dejar un comentario confirmando que fue testeado y revisado.
- Luego podrá hacer merge a `development`.

**4. Completar wiki**

- En caso de ser necesario, se completará con instrucciones, guías y aclaraciones relevantes durante el desarrollo del proyecto, con el objetivo de facilitar la colaboración entre los integrantes del equipo y mantener la documentación centralizada y actualizada.
---


## Herramientas necesarias

### IDEs
- [VSCode](https://code.visualstudio.com/)

### Entornos de ejecución backend
- [Java 21](https://adoptium.net/es/temurin/releases/?version=21&package=jdk)
- [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)

### Entornos de ejecución frontend
- [Node 22](https://nodejs.org/es/download)

### Gestión de código
- [Git](https://git-scm.com/)
- [GitHub](https://github.com/) *(requiere usuario)*

### Gestor de proyecto
- [Quire](https://quire.io/w/Reservas_ARN/33)


### TP Etapa 02
- Tareas necesarias para completar el servicio gestion-svc y reservas-svc [ETAPA02.md](./ETAPA02.md).
- Descripción paso a paso de las acciones a realizar [PRACTICA_02.pdf](PRACTICA_02.pdf)
=======
Documentación detallada (wiki)
- Estructura de carpetas e infraestructura: ver `docs/estructura.md`
- Arquitectura y modelo de datos: ver `docs/arquitectura.md`
- Guías de trabajo, convenciones y herramientas: ver `docs/guias.md`

## Login con Google (beta)

Backend (user-svc):
- Configurar el Client ID de Google como variable de entorno `GOOGLE_CLIENT_ID` (ya referenciado en `docker-compose.yml`).
- Alternativa: editar `services/user-svc/src/main/resources/application.properties` y setear `google.oauth.clientId`.
- Endpoint: `POST http://localhost:8081/auth/google` con cuerpo JSON `{ "credential": "<ID_TOKEN>" }`.

Frontend (Vite):
- Copiar `frontend/.env.example` a `frontend/.env` y completar `VITE_GOOGLE_CLIENT_ID`.
- Se agregó el script de Google Identity Services y un botón en el `Header`.
- Al autenticarse, se guarda el usuario en `localStorage` con clave `user`.

Notas:
- Esta integración verifica el ID Token de Google y crea el usuario (tipo Huesped) si no existe.
- Aún no emite JWT propio ni protege endpoints; eso puede sumarse en una etapa siguiente.
>>>>>>> Stashed changes
