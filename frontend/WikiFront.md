# 🖥️ Frontend - Reservas ARN

Guía rápida para desarrolladores sobre la arquitectura, tecnologías y estructura del frontend del proyecto **Reservas ARN**.

---

## 🚀 Tecnologías principales

| Tecnología     | Uso |
|----------------|-----|
| **React**      | Librería principal para construir la interfaz de usuario. |
| **TypeScript** | Tipado estático para mayor seguridad y mantenibilidad. |
| **Vite**       | Entorno de desarrollo rápido y moderno. |
| **Tailwind CSS** | Framework utility-first para estilos rápidos y responsivos. |
| **SCSS**       | Preprocesador CSS para crear librerías de diseño personalizadas. |
| **React Router** | Manejo de rutas y navegación en la SPA. |
| **Node.js**    | Entorno de ejecución (v20.19.0 o superior). |
| **npm / npx**  | Gestión de paquetes y ejecución de comandos. |

---

## 📁 Estructura de carpetas (`frontend/src`)

src/  
├─ assets/ # Fuentes, imágenes, SVGs  
│ ├─ fonts/  
│ └─ svg/  
├─ hooks/ # Custom hooks de React  
├─ pages/ # Vistas por ruta (Home, Auth, etc.)  
├─ routes/ # Definición de rutas de la SPA  
├─ styles/ # SCSS globales y variables  
│ ├─ base/ # Mixins, reset, typography, variables  
│ ├─ components/ # Estilos por componente (_header, _footer, etc.)  
│ ├─ pages/ # Estilos específicos de páginas  
│ └─ themes/ # Light/Dark theme  
└─ components/ # Componentes reutilizables (Botones, Cards, Forms...)  


---

## 🖼️ Assets: Imágenes, SVGs y fuentes

- **Imágenes**: `assets/images/`
- **SVGs**: `assets/svg/` → usar como componentes o importar directamente.
- **Fuentes**: `assets/fonts/` → declarar en `tailwind.config.js` o `_variables.scss`.

---

## ⚙️ Convenciones y buenas prácticas

- **SCSS**: Separa estilos globales (`base`) de componentes (`components`) y temas (`themes`).
- **React**: Cada componente tiene su propio archivo `.tsx` y opcionalmente su SCSS.
- **Pages**: Cada página corresponde a una ruta principal y puede importar componentes y estilos.
- **Assets**: Imágenes, SVGs y fuentes se colocan en `assets/`.
- **Hooks**: Hooks personalizados organizados por funcionalidad.
- **Variables y mixins**: Definidos en `styles/base/_variables.scss` y `_mixins.scss`.
- **Routing**: Todas las rutas gestionadas con React Router en `routes/`.
- **Themes**: Light y Dark themes separados en `styles/themes/`.

---



