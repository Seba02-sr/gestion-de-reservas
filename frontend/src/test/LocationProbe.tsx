import { useLocation } from "react-router-dom"

/** Muestra la ruta actual para poder afirmar sobre la navegación en los tests. */
export default function LocationProbe() {
  const l = useLocation()
  return <div data-testid="location">{l.pathname + l.search}</div>
}
