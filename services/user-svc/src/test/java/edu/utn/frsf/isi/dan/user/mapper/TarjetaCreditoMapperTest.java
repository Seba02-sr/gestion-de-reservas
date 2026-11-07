package edu.utn.frsf.isi.dan.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class TarjetaCreditoMapperTest {

  private TarjetaCreditoMapper tarjetaCreditoMapper;

  @BeforeEach
  void setUp() {
    tarjetaCreditoMapper = Mappers.getMapper(TarjetaCreditoMapper.class);
  }

  @Test
  void toEntity_DeberiaRetornarEntidad_CuandoRequestEsValido() {
    TarjetaCreditoRequest request =
        TarjetaCreditoRequest.builder()
            .numero("1234567890123456")
            .nombreTitular("Juan Perez")
            .fechaVencimiento("12/25")
            .cvc("123")
            .esPrincipal(true)
            .idBanco(1)
            .build();

    TarjetaCredito tarjetaCredito = tarjetaCreditoMapper.toEntity(request);

    assertAll(
        "Verificaciones de TarjetaCredito",
        () -> assertNotNull(tarjetaCredito),
        () -> assertNull(tarjetaCredito.getId()),
        () -> assertEquals("1234567890123456", tarjetaCredito.getNumero()),
        () -> assertEquals("Juan Perez", tarjetaCredito.getNombreTitular()),
        () -> assertEquals("12/25", tarjetaCredito.getFechaVencimiento()),
        () -> assertEquals("123", tarjetaCredito.getCvc()),
        () -> assertTrue(tarjetaCredito.isPrincipal()),
        () -> assertNotNull(tarjetaCredito.getBanco()),
        () -> assertEquals(1, tarjetaCredito.getBanco().getId()));
  }

  @Test
  void toResponse_DeberiaRetornarDTO_CuandoEntidadEsValida() {
    TarjetaCredito tarjetaCredito =
        TarjetaCredito.builder()
            .id(1)
            .numero("1234567890123456")
            .nombreTitular("Juan Perez")
            .fechaVencimiento("12/25")
            .cvc("123")
            .esPrincipal(true)
            .banco(Banco.builder().id(1).nombre("Banco Test").build())
            .build();
    TarjetaCreditoResponse response = tarjetaCreditoMapper.toResponse(tarjetaCredito);
    assertAll(
        "Verificaciones de TarjetaCreditoResponse",
        () -> assertNotNull(response),
        () -> assertEquals(1, response.id()),
        // el número sale enmascarado y el CVC no existe en la respuesta
        () -> assertEquals("**** **** **** 3456", response.numero()),
        () -> assertEquals("Juan Perez", response.nombreTitular()),
        () -> assertEquals("12/25", response.fechaVencimiento()),
        () -> assertTrue(response.esPrincipal()),
        () -> assertEquals(1, response.idBanco()));
  }

  @Test
  void toResponse_ElJsonNoContieneElCvcNiElNumeroCompleto() throws Exception {
    TarjetaCredito tarjetaCredito =
        TarjetaCredito.builder()
            .id(1)
            .numero("5500000000000004")
            .nombreTitular("Carla Gomez")
            .fechaVencimiento("12/2027")
            .cvc("987")
            .esPrincipal(true)
            .banco(Banco.builder().id(1).nombre("Banco Test").build())
            .build();

    String json =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .writeValueAsString(tarjetaCreditoMapper.toResponse(tarjetaCredito));

    assertFalse(json.contains("cvc"), json);
    assertFalse(json.contains("987"), json);
    assertFalse(json.contains("5500000000000004"), json);
    assertTrue(json.contains("**** **** **** 0004"), json);
  }

  @Test
  void updateEntityFromRequest_DeberiaActualizarCampos_CuandoRequestEsValido() {
    TarjetaCredito tarjetaCredito =
        TarjetaCredito.builder()
            .id(1)
            .numero("1234567890123456")
            .nombreTitular("Juan Perez")
            .fechaVencimiento("12/25")
            .cvc("123")
            .esPrincipal(true)
            .banco(Banco.builder().id(1).nombre("Banco Viejo").build())
            .build();

    TarjetaCreditoRequest request =
        TarjetaCreditoRequest.builder()
            .numero("6543210987654321")
            .nombreTitular("Maria Lopez")
            .fechaVencimiento("11/24")
            .cvc("456")
            .esPrincipal(false)
            .idBanco(2)
            .build();

    tarjetaCreditoMapper.updateEntityFromRequest(request, tarjetaCredito);

    assertAll(
        "Verificaciones de actualización de TarjetaCredito",
        () -> assertEquals("6543210987654321", tarjetaCredito.getNumero()),
        () -> assertEquals("Maria Lopez", tarjetaCredito.getNombreTitular()),
        () -> assertEquals("11/24", tarjetaCredito.getFechaVencimiento()),
        () -> assertEquals("456", tarjetaCredito.getCvc()),
        () -> assertFalse(tarjetaCredito.isPrincipal()),
        () -> assertNotNull(tarjetaCredito.getBanco()),
        () -> assertEquals(2, tarjetaCredito.getBanco().getId()));
  }
}
