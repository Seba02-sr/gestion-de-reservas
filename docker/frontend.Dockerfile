# syntax=docker/dockerfile:1.7
#
# Frontend de producción: Node compila el bundle de Vite y nginx (sin root) lo sirve.
# Contexto de build: ./frontend (docker compose: context: ../frontend).
#
# Las variables VITE_* se incrustan en el bundle al COMPILAR (no se leen al ejecutar):
#   VITE_API_URL=/api        -> el navegador llama a su mismo origen y nginx reenvía /api/** al gateway
#   VITE_GOOGLE_CLIENT_ID    -> vacío = sin botón de Google

# ---------------------------------------------------------------- build
FROM node:20-alpine AS build
WORKDIR /app

# Dependencias primero (capa cacheable) + cache de npm entre builds
COPY package.json package-lock.json ./
RUN --mount=type=cache,id=npm-reservas,target=/root/.npm \
    npm ci --no-audit --no-fund

ARG VITE_API_URL=/api
ARG VITE_GOOGLE_CLIENT_ID=
ENV VITE_API_URL=$VITE_API_URL \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

COPY . .
RUN npm run build

# ---------------------------------------------------------------- runtime
# nginx-unprivileged: corre como usuario no-root y escucha en el 8080 del contenedor
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null 2>&1 || exit 1
