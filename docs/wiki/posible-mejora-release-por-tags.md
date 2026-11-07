Publicar imágenes Docker al crear tags (posible mejora)

Objetivo
- Construir y publicar imágenes Docker para cada módulo cuando se crea un tag (p. ej., `v1.0.0`).
- Subirlas a GitHub Container Registry (GHCR) con tags versionados e inmutables.

Beneficios
- Versionado claro e inmutable: `vX.Y.Z` y opcional `latest`.
- Reproducibilidad: el despliegue referencia una imagen exacta.
- Buenas prácticas de CI/CD: separar tests (CI) de releases (CD).
- Útil para desarrollo y también para producción si se decide desplegar con contenedores.

Alcance (este repo)
- Módulos con Dockerfile: `services/user-svc`, `services/gestion-svc`, `services/reservas-svc`, `frontend`.
- Nombre sugerido de imagen: `ghcr.io/OWNER/REPO-<modulo>:<tag>`.

Requisitos
- Usar GHCR (incluido con GitHub). Para repos públicos es gratuito.
- Workflow con permisos para publicar paquetes (packages: write).
- Autenticación: `GITHUB_TOKEN` provisto por Actions.

Cuándo usarlo
- Releases de proyecto (p. ej., `v1.0.0`).
- Hitos de cursada (entregas) para fijar estados del sistema.

Borrador de workflow (no implementado)
```
name: Release Images

on:
  push:
    tags: [ 'v*' ]

permissions:
  contents: read
  packages: write

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [ 'user-svc', 'gestion-svc', 'reservas-svc', 'frontend' ]
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: ./services/${{ matrix.module }}
          push: true
          tags: |
            ghcr.io/${{ github.repository }}-${{ matrix.module }}:${{ github.ref_name }}
          # opcional: agregar latest solo en tags estables
          # labels: org.opencontainers.image.source=${{ github.repositoryUrl }}
```

Notas
- `github.ref_name` es el nombre del tag (p. ej., `v1.0.0`).
- Para frontend, ajustar `context` si el Dockerfile está en `frontend/` (cambiar `context` y/o incluirlo en la matriz).
- Se puede añadir otro tag `latest` si el tag cumple criterios (por ejemplo, no incluye `-rc`).
- Para multi-arquitectura (amd64/arm64), configurar `platforms` con Buildx (más lento).

Desarrollo vs Producción
- Desarrollo: facilita pruebas en entornos externos (Koyeb/Fly.io) sin rebuild local.
- Producción (si aplica): permite despliegue inmutable, rollback simple (cambiar tag), y auditoría de qué se publicó y cuándo.

Seguridad y cumplimiento (opcional)
- Agregar escaneo de vulnerabilidades (Grype/Trivy) previo al push.
- Publicar SBOM (Syft) como artefacto.

Estado actual
- Este flujo no está activado. Se documenta como mejora futura por si se decide adoptarlo.
