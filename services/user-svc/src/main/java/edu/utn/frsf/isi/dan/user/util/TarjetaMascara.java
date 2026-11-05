package edu.utn.frsf.isi.dan.user.util;

/** Enmascara el número de una tarjeta para que nunca salga completo por la API. */
public final class TarjetaMascara {

  private static final String PREFIJO = "**** **** **** ";

  private TarjetaMascara() {}

  /**
   * {@code 5500000000000004 -> **** **** **** 0004}. Solo se conservan los últimos 4 dígitos; si el
   * valor tiene menos de 4 caracteres se enmascara por completo para no filtrar nada.
   */
  public static String enmascarar(String numero) {
    if (numero == null) {
      return null;
    }
    String limpio = numero.replaceAll("\\D", "");
    if (limpio.length() < 4) {
      return "****";
    }
    return PREFIJO + limpio.substring(limpio.length() - 4);
  }
}
