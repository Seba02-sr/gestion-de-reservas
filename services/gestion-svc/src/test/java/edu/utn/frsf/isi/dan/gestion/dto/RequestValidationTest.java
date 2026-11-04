package edu.utn.frsf.isi.dan.gestion.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private static HotelRequest hotel(double lat, double lng) {
    return HotelRequest.builder()
        .nombre("Hotel Santa Fe")
        .cuit("30444444440")
        .domicilio("Bv. Pellegrini 2500")
        .latitud(lat)
        .longitud(lng)
        .telefono("342-4000000")
        .correoContacto("contacto@hotel.com")
        .categoria(4)
        .build();
  }

  private <T> Set<String> campos(T request) {
    return validator.validate(request).stream()
        .map(v -> v.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }

  @Test
  void hotel_conCoordenadasNegativasDeSantaFe_esValido() {
    assertTrue(campos(hotel(-31.6333, -60.7)).isEmpty());
  }

  @Test
  void hotel_conCoordenadasPositivasSigueSiendoValido() {
    assertTrue(campos(hotel(31.6107, 60.6735)).isEmpty());
  }

  @Test
  void hotel_conLimitesExactosEsValido() {
    assertTrue(campos(hotel(-90, -180)).isEmpty());
    assertTrue(campos(hotel(90, 180)).isEmpty());
  }

  @Test
  void hotel_conCoordenadasFueraDeRangoEsInvalido() {
    assertEquals(Set.of("latitud"), campos(hotel(-90.1, 0)));
    assertEquals(Set.of("latitud"), campos(hotel(90.1, 0)));
    assertEquals(Set.of("longitud"), campos(hotel(0, -180.1)));
    assertEquals(Set.of("longitud"), campos(hotel(0, 180.1)));
  }

  @Test
  void hotelUpdate_validaCategoriaTelefonoYCorreo() {
    assertTrue(campos(new HotelUpdateRequest(4, "342-4000000", "contacto@hotel.com")).isEmpty());
    assertEquals(
        Set.of("categoria", "telefono", "correoContacto"),
        campos(new HotelUpdateRequest(null, " ", "no-es-un-correo")));
    assertEquals(
        Set.of("categoria"), campos(new HotelUpdateRequest(-1, "342-4000000", "a@hotel.com")));
  }

  @Test
  void habitacion_numeroYPisoObligatoriosYPositivos() {
    assertTrue(
        campos(HabitacionRequest.builder().numero(1).piso(1).idTipoHabitacion(1).idHotel(1).build())
            .isEmpty());
    assertEquals(
        Set.of("numero", "piso", "idTipoHabitacion", "idHotel"),
        campos(HabitacionRequest.builder().build()));
    assertEquals(
        Set.of("numero", "piso"),
        campos(
            HabitacionRequest.builder().numero(0).piso(-2).idTipoHabitacion(1).idHotel(1).build()));
  }

  @Test
  void amenityRequest_exigeLaAmenity() {
    assertEquals(Set.of("amenity"), campos(new AmenityHotelRequest(null)));
  }
}
