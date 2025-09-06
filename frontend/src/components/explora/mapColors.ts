// Colores idénticos a _variables.scss
function readVar(name: string, fallback: string): string {
  try {
    if (typeof window === "undefined") return fallback;
    const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return v || fallback;
  } catch {
    return fallback;
  }
}

export const MAP_COLORS = {
  low:  readVar("--map-fill-low",  "#93C5FD"),  // Blue-300
  mid:  readVar("--map-fill-mid",  "#3B82F6"),  // Blue-500
  high: readVar("--map-fill-high", "#1E40AF"),  // Blue-800
  off:  readVar("--map-fill-off",  "#CBD5E1"),  // Slate-300
} as const;

// Rangos (editás solo estos dos números cuando tengas datos reales)
export const MAP_THRESHOLDS = {
  lowMax: 50,
  midMax: 150,
} as const;
