package edu.utn.frsf.isi.dan.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.user.dto.CuentaRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.CuentaBancaria;
import edu.utn.frsf.isi.dan.user.model.Propietario;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class PropietarioMapperTest {

  private PropietarioMapper propietarioMapper;

  @BeforeEach
  void setUp()
      throws NoSuchFieldException,
          SecurityException,
          IllegalArgumentException,
          IllegalAccessException {
    CuentaMapper cuentaMapper = Mappers.getMapper(CuentaMapper.class);
    PropietarioMapperImpl impl = new PropietarioMapperImpl();

    Field field = PropietarioMapperImpl.class.getDeclaredField("cuentaMapper");
    field.setAccessible(true);
    field.set(impl, cuentaMapper);

    propietarioMapper = impl;
  }

  @Test
  void toEntity_DeberiaRetornarEntidad_CuandoRequestEsValido() {
    PropietarioRequest request =
        PropietarioRequest.builder()
            .nombre("Propietario Test")
            .email("prop@email.com")
            .telefono("123456789")
            .dni("12345678")
            .username("propietario")
            .password("password123")
            .idHotel(Long.valueOf(1))
            .cuentaBancaria(CuentaRequest.builder().numeroCuenta("123456789").idBanco(1).build())
            .build();
    Propietario propietario = propietarioMapper.toEntity(request);
    assertAll(
        "Verificaciones de Propietario",
        () -> assertNotNull(propietario),
        () -> assertNull(propietario.getId()),
        () -> assertEquals("Propietario Test", propietario.getNombre()),
        () -> assertEquals("prop@email.com", propietario.getEmail()),
        () -> assertEquals("123456789", propietario.getTelefono()),
        () -> assertEquals("12345678", propietario.getDni()),
        () -> assertEquals("propietario", propietario.getUsername()),
        () -> assertNotNull(propietario.getCuentaBancaria()),
        () -> assertEquals("123456789", propietario.getCuentaBancaria().getNumeroCuenta()),
        () -> assertNotNull(propietario.getCuentaBancaria().getBanco()),
        () -> assertEquals(1, propietario.getCuentaBancaria().getBanco().getId()));
  }

  @Test
  void toResponse_DeberiaRetornarDTO_CuandoEntidadEsValida() {
    Propietario propietario =
        Propietario.builder()
            .id(1)
            .nombre("Propietario Test")
            .email("prop@email.com")
            .telefono("123456789")
            .dni("12345678")
            .username("propietario")
            .cuentaBancaria(
                CuentaBancaria.builder()
                    .id(1)
                    .numeroCuenta("123456789")
                    .banco(Banco.builder().id(1).nombre("Banco Test").build())
                    .build())
            .build();
    PropietarioResponse result = propietarioMapper.toResponse(propietario);
    assertAll(
        "Verificaciones de PropietarioResponse",
        () -> assertNotNull(result),
        () -> assertEquals(1, result.id()),
        () -> assertEquals("Propietario Test", result.nombre()),
        () -> assertEquals("prop@email.com", result.email()),
        () -> assertEquals("123456789", result.telefono()),
        () -> assertEquals("12345678", result.dni()),
        () -> assertEquals("propietario", result.username()),
        () -> assertNotNull(result.cuentaBancaria()),
        () -> assertEquals(1, result.cuentaBancaria().id()),
        () -> assertEquals("123456789", result.cuentaBancaria().numeroCuenta()),
        () -> assertEquals(1, result.cuentaBancaria().idBanco()));
  }

  @Test
  void updateEntityFromRequest_DeberiaActualizarCampos_CuandoRequestEsValido() {
    Propietario propietario =
        Propietario.builder()
            .id(1)
            .nombre("Propietario Viejo")
            .email("prop@email.com")
            .telefono("123456789")
            .dni("12345678")
            .username("propietario")
            .cuentaBancaria(
                CuentaBancaria.builder()
                    .id(1)
                    .numeroCuenta("123456789")
                    .banco(Banco.builder().id(1).nombre("Banco Viejo").build())
                    .build())
            .build();
    PropietarioRequest request =
        PropietarioRequest.builder()
            .nombre("Propietario Nuevo")
            .email("prop2@email.com")
            .telefono("987654321")
            .dni("87654321")
            .username("propietario2")
            .password("newpassword123")
            .idHotel(Long.valueOf(2))
            .cuentaBancaria(CuentaRequest.builder().numeroCuenta("987654321").idBanco(2).build())
            .build();
    propietarioMapper.updateEntityFromRequest(request, propietario);
    assertAll(
        "Verificaciones de actualización de Propietario",
        () -> assertNotNull(propietario),
        () -> assertEquals(1, propietario.getId()),
        () -> assertEquals("Propietario Nuevo", propietario.getNombre()),
        () -> assertEquals("prop2@email.com", propietario.getEmail()),
        () -> assertEquals("987654321", propietario.getTelefono()),
        () -> assertEquals("87654321", propietario.getDni()),
        () -> assertEquals("propietario2", propietario.getUsername()),
        () -> assertNotNull(propietario.getCuentaBancaria()),
        () -> assertEquals("987654321", propietario.getCuentaBancaria().getNumeroCuenta()),
        () -> assertNotNull(propietario.getCuentaBancaria().getBanco()),
        () -> assertEquals(2, propietario.getCuentaBancaria().getBanco().getId()));
  }
}
