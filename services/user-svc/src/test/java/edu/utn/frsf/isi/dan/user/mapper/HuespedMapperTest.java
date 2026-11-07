package edu.utn.frsf.isi.dan.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.HuespedUpdateRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class HuespedMapperTest {

  private HuespedMapper huespedMapper;

  @BeforeEach
  void setUp() {
    TarjetaCreditoMapper tarjetaMapper = Mappers.getMapper(TarjetaCreditoMapper.class);
    huespedMapper = new HuespedMapperImpl(tarjetaMapper);
  }

  @Test
  void toEntity_DeberiaRetornarEntidad_CuandoRequestEsValido() {
    HuespedRequest request =
        HuespedRequest.builder()
            .nombre("Juan")
            .email("juanPerez@email.com")
            .telefono("123456789")
            .dni("12345678")
            .username("juanperez")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetasCredito(
                List.of(TarjetaCreditoRequest.builder().numero("1234567890123456").build()))
            .build();
    Huesped huesped = huespedMapper.toEntity(request);

    assertAll(
        "Verificaciones de Huesped",
        () -> assertNotNull(huesped),
        () -> assertNull(huesped.getId()),
        () -> assertEquals("Juan", huesped.getNombre()),
        () -> assertEquals("juanPerez@email.com", huesped.getEmail()),
        () -> assertEquals("123456789", huesped.getTelefono()),
        () -> assertEquals("12345678", huesped.getDni()),
        () -> assertEquals("juanperez", huesped.getUsername()),
        () -> assertEquals(LocalDate.of(1990, 1, 1), huesped.getFechaNacimiento()),
        () -> assertNotNull(huesped.getTarjetaCredito()),
        () -> assertFalse(huesped.getTarjetaCredito().isEmpty()));
  }

  @Test
  void toResponse_DeberiaRetornarDTO_CuandoEntidadEsValida() {
    Huesped huesped =
        Huesped.builder()
            .id(1)
            .nombre("Juan")
            .email("juanPerez@email.com")
            .telefono("123456789")
            .dni("12345678")
            .username("juanperez")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetaCredito(List.of())
            .build();
    HuespedResponse result = huespedMapper.toResponse(huesped);
    assertAll(
        "Verificaciones de HuespedResponse",
        () -> assertNotNull(result),
        () -> assertEquals(1, result.id()),
        () -> assertEquals("Juan", result.nombre()),
        () -> assertEquals("juanPerez@email.com", result.email()),
        () -> assertEquals("123456789", result.telefono()),
        () -> assertEquals("12345678", result.dni()),
        () -> assertEquals("juanperez", result.username()),
        () -> assertEquals(LocalDate.of(1990, 1, 1), result.fechaNacimiento()),
        () -> assertTrue(result.tarjetaCredito().isEmpty()));
  }

  private Huesped huespedExistente() {
    TarjetaCredito tarjeta = TarjetaCredito.builder().id(5).esPrincipal(true).build();
    return Huesped.builder()
        .id(1)
        .nombre("Juan")
        .email("juanPerez@gmail.com")
        .telefono("123456789")
        .dni("12345678")
        .username("juanperez")
        .password("hash")
        .fechaNacimiento(LocalDate.of(1990, 1, 1))
        .tarjetaCredito(new ArrayList<>(List.of(tarjeta)))
        .build();
  }

  @Test
  void updateEntityFromRequest_DeberiaActualizarDatosSinTocarTarjetasNiPassword() {
    Huesped huesped = huespedExistente();
    List<TarjetaCredito> tarjetasOriginales = huesped.getTarjetaCredito();
    HuespedUpdateRequest request =
        HuespedUpdateRequest.builder()
            .nombre("Juan Actualizado")
            .email("juan@gmail.com")
            .telefono("987654321")
            .dni("87654321")
            .fechaNacimiento(LocalDate.of(1992, 2, 2))
            .build();

    huespedMapper.updateEntityFromRequest(request, huesped);
    assertAll(
        "Verificaciones de actualización de Huesped",
        () -> assertEquals(1, huesped.getId()),
        () -> assertEquals("Juan Actualizado", huesped.getNombre()),
        () -> assertEquals("juan@gmail.com", huesped.getEmail()),
        () -> assertEquals("987654321", huesped.getTelefono()),
        () -> assertEquals("87654321", huesped.getDni()),
        () -> assertEquals(LocalDate.of(1992, 2, 2), huesped.getFechaNacimiento()),
        () -> assertEquals("juanperez", huesped.getUsername()),
        () -> assertEquals("hash", huesped.getPassword()),
        () -> assertSame(tarjetasOriginales, huesped.getTarjetaCredito()),
        () -> assertEquals(1, huesped.getTarjetaCredito().size()));
  }

  @Test
  void updateEntityFromRequest_SoloTelefono_NoModificaElResto() {
    Huesped huesped = huespedExistente();

    huespedMapper.updateEntityFromRequest(
        HuespedUpdateRequest.builder().telefono("555").build(), huesped);

    assertAll(
        () -> assertEquals("555", huesped.getTelefono()),
        () -> assertEquals("Juan", huesped.getNombre()),
        () -> assertEquals("juanPerez@gmail.com", huesped.getEmail()),
        () -> assertEquals("12345678", huesped.getDni()),
        () -> assertEquals(LocalDate.of(1990, 1, 1), huesped.getFechaNacimiento()),
        () -> assertEquals(1, huesped.getTarjetaCredito().size()));
  }
}
