Pipeline CI: pasos y fallos comunes

Qué corre el workflow `.github/workflows/ci.yml`
- Spotless check (formato): `mvn -ntp spotless:check`
- Tests Maven: `mvn -ntp test`
- Dependency Review (solo PRs): análisis de dependencias con GitHub Advisory DB

Posibles fallos y cómo resolverlos
- Formato (Spotless) falla:
  - Síntoma: job "Spotless check (format)" marca diferencias.
  - Solución: `mvn spotless:apply`, revisá y commiteá los cambios.

- Tests fallan:
  - Síntoma: job "Run tests" falla en uno o más módulos.
  - Causas típicas: aserciones rotas, datos de prueba inconsistentes, cambios de esquema.
  - Si usa Testcontainers: puede fallar si no puede acceder al daemon de Docker.
    - En runners GitHub suele funcionar de fábrica; si no, ver logs de "Cannot connect to Docker".
  - Solución: corregir tests/lógica; si es infraestructura (Testcontainers), revisar configuración de imágenes y puertos.

- Revisión de dependencias alerta vulnerabilidades (PRs):
  - Síntoma: comentarios/alertas en el PR con CVEs o licencias.
  - Estado: informativo (no bloquea). Actualizá a versiones seguras cuando sea posible.

Consejos
- Ejecutá localmente `mvn spotless:apply test` antes de abrir un PR.
- Commits pequeños y bien descritos facilitan encontrar la causa de fallos.

---

## Jobs adicionales (ticket 7000)

El workflow `.github/workflows/ci.yml` ahora tiene cuatro jobs:

| Job | Qué hace |
|---|---|
| `test-backend` | Spotless check + `mvn test` (Java 21) |
| `frontend` | `npm ci`, `typecheck`, `lint`, `test` y `build` del frontend (Node 20) |
| `docker-build` | Valida `docker/docker-compose.yml` con todos los perfiles (`config -q`) y el `include` de la raíz, y **construye las 6 imágenes** (`docker/backend.Dockerfile` ×5 + `docker/frontend.Dockerfile`) con `docker/bake-action` y cache de GitHub Actions. No levanta el stack. |
| `dependency-review` | Análisis de dependencias (solo PRs) |

Si `docker-build` falla: reproducir con `docker compose -f docker/docker-compose.yml build`.
