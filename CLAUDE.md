# CLAUDE.md

Guía para trabajar en este repo (TP de *Desarrollo de Aplicaciones en la Nube*, UTN FRSF). Fork de la cátedra: https://github.com/dan-frsf/tp-2025 (remote `catedra`, solo referencia).

## Estructura del repo

Monorepo Maven multi-módulo (Spring Boot, Java 21) + frontend.

```
pom.xml                      # parent (tp2025)
common/dan-common-lib/       # librería compartida (DTOs/eventos de mensajería)
services/user-svc/           # usuarios (MySQL)            -> 8081
services/reservas-svc/       # reservas (MongoDB)          -> 8082
services/gestion-svc/        # hoteles/habitaciones/tarifas (PostgreSQL) -> 8083
frontend/                    # React + Vite
infra/                       # configs montadas por docker (mysql, postgres/initdb, pgadmin, phpmyadmin, rabbitmq)
docker-compose.yml           # compose que se usa (raíz)
docs/                        # documentación del proyecto
ETAPA01.md, ETAPA02.md       # enunciados; PRACTICA_02.pdf, TP_PARTE_02.pdf
```

`infra/docker-compose.yml` es una versión anterior/legada (sin profiles ni frontend): **el compose vigente es el de la raíz**.

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
./mvnw -DskipTests clean package     # solo JARs (los Dockerfile copian target/*.jar)
./mvnw spotless:apply                # formatear (obligatorio antes de commitear)

cp .env.example .env                 # elegir COMPOSE_PROFILES (ver README)
docker compose up -d --build         # levanta lo que digan los profiles
docker compose down                  # o `down -v` para borrar volúmenes
```

Profiles del compose raíz: `frontend`, `prod`, `infra`, `user`, `reservas`, `gestion`. Ej. todo: `COMPOSE_PROFILES=frontend,infra,user,reservas,gestion`.

Tests de un servicio: `./mvnw -pl services/gestion-svc test`.

> **Windows/OneDrive:** el repo está dentro de OneDrive y a veces `clean` falla con "Failed to delete ...\target\classes\..." por un lock (OneDrive o el language server de Java). Solución: borrar los `target/` a mano y reintentar.

### MySQL (user-svc): el esquema lo crea Hibernate
El compose raíz **no monta `infra/mysql/initdb`** en el servicio `mysql` (solo el volumen `mysql_data`), así que las tablas las crea Hibernate con `spring.jpa.hibernate.ddl-auto=update`. `infra/mysql/initdb/01_schema.sql` es solo referencia (alineado con las entidades) y su `ON DELETE CASCADE` **no existe** en la base real: el borrado de un huésped elimina sus tarjetas únicamente por el cascade JPA (`Huesped.tarjetaCredito`: `cascade = ALL` + `orphanRemoval`), verificado en `HuespedBorradoCascadaTest` (H2). No hay que depender de FKs de la base para esa regla.

### initdb y volúmenes (PostgreSQL y demás bases que sí montan initdb)
Los scripts de `infra/*/initdb` (p. ej. `infra/postgres/initdb/01_schema.sql`) solo se ejecutan cuando el volumen de la base está vacío. **Si cambia un script de initdb hay que recrear el volumen**: `docker compose down -v` (borra todo) o borrar solo el volumen de esa base (`docker volume ls` → `docker volume rm <proyecto>_postgres_data`, `..._mysql_data`, `..._mongo_data`).

### PostgreSQL (gestion-svc) y mensajería de habitaciones
- `infra/postgres/initdb/01_schema.sql` **sí** se monta y gestion-svc corre con `ddl-auto=none`: la columna `habitacion.disponible` y la restricción `UNIQUE (id_hotel, numero)` (`uq_habitacion_hotel_numero`) solo existen si se recrea el volumen (`docker compose down -v` o `docker volume rm <proyecto>_postgres_data`).
- gestion-svc publica en el exchange topic `dan.exchange` con routing key `dan.habitacion.<accion>` (`crear`, `actualizar`, `eliminar`, `cerrar-hotel`); reservas-svc escucha la cola `habitacion.topic` (binding `dan.habitacion.#`, ver `infra/rabbitmq/definitions.json`). Propiedades: `rabbitmq.exchange` y `rabbitmq.routingkey.prefix`.
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
  2. Recrear reservas-svc con una fecha posterior al check-out. Bash: `APP_FECHA_ACTUAL=2027-01-15 docker compose up -d reservas-svc`. PowerShell: `$env:APP_FECHA_ACTUAL="2027-01-15"; docker compose up -d reservas-svc`. (También se puede poner `APP_FECHA_ACTUAL=...` en `.env`.) El log muestra un WARN "FECHA SIMULADA".
  3. Probar `PUT /reservas/{id}/review-cliente` y `PATCH /reservas/{id}/estado` con `FINALIZADA`.
  4. Volver a la fecha real: recrear el servicio sin la variable (`Remove-Item Env:APP_FECHA_ACTUAL` / `unset APP_FECHA_ACTUAL`, y sacarla de `.env`; luego `docker compose up -d reservas-svc`).
  Con la fecha simulada el reloj queda fijo ese día a las 12:00; no uses la fecha simulada en un entorno real.

## Puertos

| Servicio | Puerto host |
|---|---|
| user-svc | 8081 |
| reservas-svc | 8082 |
| gestion-svc | 8083 |
| Frontend (dev/prod) | 8080 |
| MySQL / phpMyAdmin | 3306 / 6080 |
| PostgreSQL / pgAdmin | 5432 / 6081 |
| MongoDB / Mongo Express | 27017 / 6091 |
| RabbitMQ / consola | 5672 / 15672 (admin/admin) |

(Dentro de docker los servicios escuchan en 8080. En la cátedra, el gateway ocupa el 8080 y Eureka el 8761; aún no están integrados acá.)

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
Referencia en `catedra/feature/tp03` (Spring Cloud Gateway), `tp04` (Eureka + OpenFeign: reservas-svc valida el huésped contra user-svc) y `tp05` (observabilidad con OpenTelemetry/Loki/Tempo/Prometheus/Grafana, contiene tp03 y tp04). Docs: ETAPA03.md, ETAPA04.md, ETAPA04-IMPL.md, ETAPA05-OBSERVABILIDAD.md.
