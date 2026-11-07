// Validaciones de cliente que espejan las anotaciones de HuespedRequest / TarjetaCreditoRequest.
// El backend vuelve a validar y es el que decide.

export interface DatosRegistro {
  nombre: string
  email: string
  telefono: string
  dni: string
  username: string
  password: string
  fechaNacimiento: string
  numero: string
  nombreTitular: string
  fechaVencimiento: string
  cvc: string
  idBanco: string
}

export type ErroresRegistro = Partial<Record<keyof DatosRegistro, string>>

export function validarRegistro(d: DatosRegistro, hoy: string): ErroresRegistro {
  const e: ErroresRegistro = {}
  if (d.nombre.trim().length < 2) e.nombre = "Ingresá tu nombre (mínimo 2 letras)"
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.email)) e.email = "Email inválido"
  if (!d.telefono.trim()) e.telefono = "Ingresá un teléfono"
  if (!d.dni.trim()) e.dni = "Ingresá tu DNI"
  if (!d.username.trim()) e.username = "Elegí un usuario"
  if (d.password.length < 4) e.password = "La contraseña debe tener al menos 4 caracteres"
  if (!d.fechaNacimiento) e.fechaNacimiento = "Ingresá tu fecha de nacimiento"
  else if (d.fechaNacimiento >= hoy) e.fechaNacimiento = "La fecha de nacimiento debe ser pasada"

  const numero = d.numero.replace(/\s+/g, "")
  if (!/^\d{13,19}$/.test(numero)) e.numero = "El número de tarjeta debe tener entre 13 y 19 dígitos"
  if (!d.nombreTitular.trim()) e.nombreTitular = "Ingresá el titular"
  if (!/^(0[1-9]|1[0-2])\/(\d{2}|\d{4})$/.test(d.fechaVencimiento)) e.fechaVencimiento = "Formato MM/AA"
  if (!/^\d{3,4}$/.test(d.cvc)) e.cvc = "El código tiene 3 o 4 dígitos"
  if (!d.idBanco) e.idBanco = "Elegí el banco emisor"
  return e
}
