# 🖥️ Frontend - Reservas ARN

Guía rápida para desarrolladores sobre la arquitectura, tecnologías y estructura del frontend del proyecto **Reservas ARN**.

---

## 🚀 Tecnologías principales

| Tecnología       | Uso |
|------------------|-----|
| React            | Librería principal para construir la interfaz de usuario |
| TypeScript       | Tipado estático para mayor seguridad y mantenibilidad |
| Vite             | Entorno de desarrollo rápido y moderno (build + dev server) |
| Tailwind CSS     | Utility-first; usamos `@apply` dentro de SCSS para layout/espaciados |
| SCSS             | Sistema de diseño propio: variables, mixins, tipografías |
| React Router     | Ruteo SPA |
| Node.js / npm    | Node 20.x recomendado; npm para scripts |

---

## 📁 Estructura de carpetas (`frontend/src`)

src/
- assets/
  - fonts/
  - images/
  - svg/
- components/
  - benefits/
  - explora/
  - hero/
  - hoteles_destacados/
  - layout/ (Header, Footer)
  - ui/ (Section, SectionHeader, Button, Card, Field)
  - icons/ (SVGs como componentes React)
- scss/
  - base/ (variables, mixins, typography, reset, layout)
  - components/ (hero, benefits, hotel-card, explora-arg, searchbar, button, card, mapa-arg, form, section)
  - layout/ (header, footer)
  - pages/
  - themes/ (light, dark)
- tailwind.scss (carga de Tailwind base/components/utilities)
- main.tsx (punto de entrada React)

Notas
- Alias `@` apunta a `src` (ver `vite.config.ts`).
- Estilos unificados: `rarn-section` y `rarn-section__container` via mixins `container()` y `section-spacing()`.

---

## 🖼️ Assets: Imágenes, SVGs y fuentes

- Imágenes: `src/assets/images/`
- SVGs: `src/assets/svg/` o `src/components/icons/` como componentes React (ej.: `ShieldIcon`).
- Fuentes: `src/assets/fonts/` → declaradas en `scss/base/_variables.scss`.

---

## ⚙️ Convenciones y buenas prácticas

- SCSS: tokens globales en `scss/base/_variables.scss`; mixins en `scss/base/_mixins.scss` y `scss/base/_layout.scss`.
- Tailwind: se aplica vía `@apply` en SCSS para layout/espaciados; colores/typo se manejan con variables SCSS.
- BEM + prefijo: clases de componentes con prefijo `rarn-...` para consistencia y aislación.
- Componentes: cada `.tsx` puede tener su SCSS en `scss/components/` si aplica.
- Ruteo: React Router; SPA servida con fallback Nginx.
- Alias: preferir `@/` para imports desde `src`.

---

## 🧰 Puesta en marcha (dev local)

Requisitos
- Node.js 20.x
- npm 10.x

Pasos
- `cd frontend`
- `npm ci`
- `npm run dev`
- Abrir `http://localhost:5173`

Build local y preview
- `npm run build`
- `npm run preview` → `http://localhost:4173`

---

## 🐳 Ejecución con Docker Compose (producción)

Requisitos
- Docker Desktop o Docker Engine + Docker Compose plugin

Levantar todo (backend + frontend), desde la raíz del repo
- `docker compose -f docker/docker-compose.yml up -d --build`
- Abrir `http://localhost:5173` (nginx sirve el build y reenvía `/api/**` al gateway)

Rebuild rápido tras cambios en el front
- `docker compose -f docker/docker-compose.yml build frontend && docker compose -f docker/docker-compose.yml up -d frontend`

Desarrollo con recarga en caliente (Vite)
- `docker compose -f docker/docker-compose.yml --profile dev up -d frontend-dev` → `http://localhost:5174`

Estructura
- `docker/frontend.Dockerfile`: build multi-stage (Node → nginx sin root); `VITE_*` entran como build args
- `frontend/nginx.conf`: proxy `/api/` al gateway, SPA fallback y cache de estáticos
- `docker/docker-compose.yml`: servicios `frontend` (producción, 5173) y `frontend-dev` (perfil `dev`, 5174)

Troubleshooting
- Si ves “Welcome to nginx”, reconstruí el build: `docker compose -f docker/docker-compose.yml build --no-cache frontend`
- Ver logs: `docker compose -f docker/docker-compose.yml logs -f frontend`

---

## 🔧 Configuración y otras notas

- Vite: `vite.config.ts` usa alias `@` → `src`.
- Tailwind: `tailwind.config.js` incluye `src/**/*.{tsx,scss}`.
- Mapas: colores por rango expuestos como CSS vars (`--map-fill-low/mid/high/off`).
- Accesibilidad: focus ring con token `rarn-color-focus-ring`.

