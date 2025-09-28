// main.tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './scss/main.scss' // <--- Importa SCSS aquí
import App from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
