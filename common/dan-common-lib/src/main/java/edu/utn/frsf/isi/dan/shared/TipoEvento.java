package edu.utn.frsf.isi.dan.shared;

public enum TipoEvento {
  CREAR,
  ACTUALIZAR_DATOS,
  ACTUALIZAR_PRECIO,
  ELIMINAR,
  /**
   * Un hotel se cerró: el evento trae el id del hotel, los ids de todas sus habitaciones y la fecha
   * de cierre. El consumidor debe crear para cada habitación una reserva CERRADA con check-in en
   * {@code fechaCierre} y check-out null.
   */
  CERRAR_HOTEL
}
