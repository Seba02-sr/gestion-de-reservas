# Estructura del repositorio

Monorepo: frontend, microservicios, librería común y toda la infraestructura de Docker conviven en este repositorio.

```
pom.xml                      # parent Maven (reactor)
common/
  dan-common-lib/            # DTOs y eventos de mensajería compartidos
  dan-spring-gateway/        # API Gateway (Spring Cloud Gateway WebMVC)   -> 8080
  dan-eureka-server/         # Service discovery (Eureka Server)           -> 8761
services/
  user-svc/                  # usuarios, huéspedes, tarjetas, bancos, login (MySQL)
  gestion-svc/               # hoteles, habitaciones, tarifas (PostgreSQL) -> publica eventos
  reservas-svc/              # reservas y búsqueda de habitaciones (MongoDB) -> consume eventos
frontend/                    # SPA React + Vite (en producción, nginx)
docker/                      # TODO lo necesario para levantar el proyecto con Docker
  docker-compose.yml         #   compose principal (perfiles: observability, tools, dev)
  docker-compose.dev-ports.yml  # override opcional: publica las bases en el host (desarrollo)
  backend.Dockerfile         #   multi-etapa: Maven+JDK 21 -> un target JRE 21 por servicio
  frontend.Dockerfile        #   Node (build) -> nginx sin root
  config/                    #   configuración montada en los contenedores
    rabbitmq/ postgres/initdb/ mysql/ pgadmin/ phpmyadmin/
    otel/ tempo/ loki/ prometheus/ grafana/
docker-compose.yml           # solo `include` de docker/docker-compose.yml (para `docker compose up` desde la raíz)
docs/                        # documentación (esta carpeta) y colección Postman
scripts/                     # generar-trafico.ps1
```

Cómo levantar todo: ver el [README](../README.md). Guía de desarrollo: [desarrollo.md](desarrollo.md).
