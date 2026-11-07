/**
 * Devuelve la fecha de hoy en formato YYYY-MM-DD (ISO)
 * para usar en <input type="date" min="...">
 */
export function todayISO(): string {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * Verifica si una fecha (YYYY-MM-DD) es válida respecto a hoy.
 */
export function isPastDate(date: string): boolean {
  return !!date && date < todayISO();
}

/**
 * Verifica si el check-out es posterior al check-in.
 */
export function isInvalidCheckOut(checkIn: string, checkOut: string): boolean {
  if (!checkOut) return false;
  const minCO = checkIn || todayISO();
  return checkOut < minCO;
}

/** Cantidad de noches entre dos fechas YYYY-MM-DD (0 si falta alguna o son inválidas). */
export function nochesEntre(checkIn?: string, checkOut?: string): number {
  if (!checkIn || !checkOut) return 0;
  const a = Date.parse(`${checkIn}T00:00:00Z`);
  const b = Date.parse(`${checkOut}T00:00:00Z`);
  if (Number.isNaN(a) || Number.isNaN(b) || b <= a) return 0;
  return Math.round((b - a) / 86_400_000);
}

/** 2026-12-10 -> 10/12/2026 */
export function formatFecha(iso?: string | null): string {
  if (!iso) return "";
  const [y, m, d] = iso.slice(0, 10).split("-");
  return `${d}/${m}/${y}`;
}
