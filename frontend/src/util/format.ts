// src/utils/format.ts
export const formatCurrencyARS = (value: number) =>
  value.toLocaleString("es-AR", { style: "currency", currency: "ARS", maximumFractionDigits: 0 })
