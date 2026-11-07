// DTOs reales del backend (ver los records de cada servicio). Los nombres coinciden con el JSON.

// ---------- user-svc ----------
export interface AuthResponse {
  userId: number
  email: string
  nombre?: string | null
  givenName?: string | null
  familyName?: string | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface BancoResponse {
  id: number
  nombre: string
  activo?: boolean
}

export interface TarjetaCreditoRequest {
  numero: string
  nombreTitular: string
  /** MM/YY o MM/YYYY */
  fechaVencimiento: string
  cvc: string
  esPrincipal: boolean
  idBanco: number
}

export interface HuespedRequest {
  nombre: string
  email: string
  telefono: string
  dni: string
  username: string
  password: string
  /** yyyy-MM-dd */
  fechaNacimiento: string
  tarjetasCredito: TarjetaCreditoRequest[]
}

export interface TarjetaCreditoResponse {
  id: number
  /** enmascarado: **** **** **** 0004 */
  numero: string
  nombreTitular: string
  fechaVencimiento: string
  esPrincipal: boolean
  idBanco: number
}

export interface HuespedResponse {
  id: number
  nombre: string
  email: string
  telefono: string
  dni: string
  username: string
  fechaNacimiento: string
  tarjetaCredito: TarjetaCreditoResponse[]
}

// ---------- gestion-svc ----------
export interface HotelResponse {
  id: number
  nombre: string
  cuit?: string
  domicilio: string
  latitud: number
  longitud: number
  telefono?: string
  correoContacto?: string
  categoria: number
  cerrado: boolean
}

// ---------- reservas-svc ----------
export interface HotelResumen {
  id: number
  nombre: string
  categoria: number
  domicilio: string
  latitud: number
  longitud: number
}

export interface HabitacionResponse {
  habitacionId: number
  numero: number
  piso: number
  disponible: boolean
  capacidad: number
  precioNoche: number | null
  amenities: string[]
  idTipoHabitacion: number
  tipoHabitacion: string
  hotel: HotelResumen
}

export interface BusquedaHabitaciones {
  checkIn?: string
  checkOut?: string
  cantidadHuespedes?: number
  precioMin?: number
  precioMax?: number
  estrellas?: number
  amenities?: string[]
  lat?: number
  lng?: number
  radioKm?: number
  hotelId?: number
}

export type EstadoReserva =
  | "RESERVADA"
  | "CONFIRMADA"
  | "CANCELADA"
  | "FINALIZADA"
  | "BLOQUEADA"
  | "ADEUDADA"
  | "CERRADA"

export interface Pago {
  method: string
  transactionId?: string | null
  amount: { precio: number; moneda: string }
  status: string
}

export interface Review {
  rating: number
  comment?: string | null
  createdAt?: string
}

export interface ReservaResponse {
  id: string
  habitacionId: number
  hotelId: number
  idHuesped: string
  nombreHuesped?: string
  emailHuesped?: string
  checkIn: string
  checkOut: string
  createdAt: string
  precioNoche: number
  precioTotal: number
  totalPagado: number
  saldo: number
  estado: EstadoReserva
  pagos: Pago[]
  clientReview?: Review | null
  hostReview?: Review | null
}

export interface ReservaRequest {
  habitacionId: number
  idHuesped: number
  checkIn: string
  checkOut: string
}

export interface PagoRequest {
  method: string
  monto: number
  transactionId?: string
}

export interface ReviewRequest {
  rating: number
  comentario?: string
}
