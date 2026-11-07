# Multi-stage build for Vite React app

# 1) Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies first (better layer caching)
COPY package*.json ./
RUN npm ci --no-audit --no-fund

# Las variables VITE_* se incrustan en el bundle al COMPILAR: hay que pasarlas como build args.
#  - VITE_API_URL=/api  -> el navegador llama al mismo origen y nginx reenvía /api/** al gateway
#  - VITE_GOOGLE_CLIENT_ID vacío -> el botón de Google no aparece
ARG VITE_API_URL=/api
ARG VITE_GOOGLE_CLIENT_ID=
ENV VITE_API_URL=$VITE_API_URL \
    VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

# Copy sources and build
COPY . .
RUN npm run build

# 2) Minimal Nginx to serve static files
FROM nginx:1.27-alpine AS runner

# Replace default site with SPA-friendly config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]

