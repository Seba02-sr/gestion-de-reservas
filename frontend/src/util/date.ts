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
