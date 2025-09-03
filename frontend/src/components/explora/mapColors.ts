// Colores idénticos a _variables.scss
export const MAP_COLORS = {
  low:  "#2dd4bf",     // Turquesa (bajo) → fresco
  mid:  "#3b82f6",     // Azul (medio) → estable
  high: "#eab308",     // ✅ Dorado (`#eab308`) → riqueza, valor, positivo
  off:  "#94a3b8",     // Gris más oscuro para mejor contraste
} as const;

// Rangos (editás solo estos dos números cuando tengas datos reales)
export const MAP_THRESHOLDS = {
  lowMax: 50,
  midMax: 150,
} as const;
