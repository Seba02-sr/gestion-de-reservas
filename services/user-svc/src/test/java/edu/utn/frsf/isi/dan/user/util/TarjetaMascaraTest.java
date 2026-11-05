package edu.utn.frsf.isi.dan.user.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TarjetaMascaraTest {

  @Test
  void conservaSoloLosUltimosCuatroDigitos() {
    assertEquals("**** **** **** 0004", TarjetaMascara.enmascarar("5500000000000004"));
    assertEquals("**** **** **** 1111", TarjetaMascara.enmascarar("4111111111111111"));
  }

  @Test
  void ignoraEspaciosYGuiones() {
    assertEquals("**** **** **** 0004", TarjetaMascara.enmascarar("5500 0000 0000 0004"));
    assertEquals("**** **** **** 0004", TarjetaMascara.enmascarar("5500-0000-0000-0004"));
  }

  @Test
  void numeroMuyCortoSeEnmascaraPorCompleto() {
    assertEquals("****", TarjetaMascara.enmascarar("123"));
    assertEquals("****", TarjetaMascara.enmascarar(""));
  }

  @Test
  void nullDevuelveNull() {
    assertNull(TarjetaMascara.enmascarar(null));
  }
}
