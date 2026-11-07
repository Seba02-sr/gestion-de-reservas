package edu.utn.frsf.isi.dan.gestion.demo;

/**
 * Calcula el dígito verificador del CUIT con el mismo algoritmo que {@code @Mod11Check(threshold =
 * 7)}.
 */
final class Cuit {

  private Cuit() {}

  /**
   * Completa una base de 10 dígitos con su dígito verificador. Si el dígito resultaría 10 (CUIT
   * inexistente) se incrementa la base hasta obtener uno válido, de forma determinística.
   */
  static String completar(String base10) {
    if (base10 == null || !base10.matches("\\d{10}")) {
      throw new IllegalArgumentException("La base del CUIT debe tener 10 dígitos: " + base10);
    }
    long base = Long.parseLong(base10);
    while (true) {
      String s = String.format("%010d", base);
      int dv = digitoVerificador(s);
      if (dv != 10) {
        return s + dv;
      }
      base++;
    }
  }

  /**
   * Pesos 2,3,4,5,6,7,2,3,... de derecha a izquierda; verificador = 11 - (suma mod 11), 11 -> 0.
   */
  static int digitoVerificador(String digitos) {
    int suma = 0;
    int peso = 2;
    for (int i = digitos.length() - 1; i >= 0; i--) {
      suma += (digitos.charAt(i) - '0') * peso;
      peso = peso == 7 ? 2 : peso + 1;
    }
    int dv = 11 - (suma % 11);
    return dv == 11 ? 0 : dv;
  }
}
