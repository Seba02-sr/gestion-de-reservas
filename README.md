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
