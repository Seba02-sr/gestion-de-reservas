# Estructura del Repositorio e Infraestructura

## Organización de directorios
Proyecto en monorepo: frontend, microservicios y scripts de infraestructura conviven en este repo.

- `frontend/`: aplicación web (Vite + Nginx para prod)
- `services/`: microservicios Spring Boot
  - `user-svc/`, `reservas-svc/`, `gestion-svc/`
- `infra/`: docker-compose específico de infraestructura (histórico)
  - Bases de datos, UIs y colas (MySQL, PostgreSQL, MongoDB, RabbitMQ, phpMyAdmin, pgAdmin, Mongo Express)

Actualmente, todo se puede levantar desde el compose raíz con perfiles (ver README).

## Comandos útiles (infra histórica)

Levantar sólo MySQL y phpMyAdmin desde `infra/`:
```bash
docker compose -f infra/docker-compose.yml up -d mysql phpmyadmin
```

Bajar todo lo de `infra/`:
```bash
docker compose -f infra/docker-compose.yml down
```

Bajar y borrar volúmenes (datos):
```bash
docker compose -f infra/docker-compose.yml down -v
```

