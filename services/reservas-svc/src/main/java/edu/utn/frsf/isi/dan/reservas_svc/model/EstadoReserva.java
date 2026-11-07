package edu.utn.frsf.isi.dan.reservas_svc.model;

public enum EstadoReserva {
  CONFIRMADA,
  RESERVADA,
  CANCELADA,
  FINALIZADA,
  BLOQUEADA,
  ADEUDADA,
  /** El hotel cerró: la habitación no se ofrece (reserva abierta, sin fecha de salida). */
  CERRADA
}
