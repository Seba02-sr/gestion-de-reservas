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

Documentación detallada (wiki)
- Estructura de carpetas e infraestructura: ver `docs/estructura.md`
- Arquitectura y modelo de datos: ver `docs/arquitectura.md`
- Guías de trabajo, convenciones y herramientas: ver `docs/guias.md`
