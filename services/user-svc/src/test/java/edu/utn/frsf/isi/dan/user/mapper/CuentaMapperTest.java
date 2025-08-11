package edu.utn.frsf.isi.dan.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

import edu.utn.frsf.isi.dan.user.dto.CuentaRequest;
import edu.utn.frsf.isi.dan.user.dto.CuentaResponse;
import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.CuentaBancaria;

public class CuentaMapperTest {

    private CuentaMapper cuentaMapper;

    @BeforeEach
    void setUp() {
        cuentaMapper = Mappers.getMapper(CuentaMapper.class);
    }

    @Test
    void toEntity_DeberiaRetornarEntidad_CuandoRequestEsValido() {
        CuentaRequest request = CuentaRequest.builder()
                .idBanco(1)
                .numeroCuenta("123456789")
                .build();

        CuentaBancaria cuentaBancaria = cuentaMapper.toEntity(request);

        assertAll("Verificaciones de CuentaBancaria",
                () -> assertNotNull(cuentaBancaria),
                () -> assertNull(cuentaBancaria.getId()),
                () -> assertEquals("123456789", cuentaBancaria.getNumeroCuenta()),
                () -> assertNotNull(cuentaBancaria.getBanco()),
                () -> assertEquals(1, cuentaBancaria.getBanco().getId()),
                () -> assertNull(cuentaBancaria.getPropietario()));
    }

    @Test
    void toResponse_DeberiaRetornarDTO_CuandoEntidadEsValida() {
        CuentaBancaria cuentaBancaria = CuentaBancaria.builder()
                .id(1)
                .numeroCuenta("123456789")
                .cbu("1234567890123456789012")
                .alias("aliasTest")
                .banco(Banco.builder().id(1).nombre("Banco Test").build())
                .build();
        CuentaResponse result = cuentaMapper.toResponse(cuentaBancaria);

        assertAll("Verificaciones de CuentaResponse",
                () -> assertNotNull(result),
                () -> assertEquals(1, result.id()),
                () -> assertEquals("123456789", result.numeroCuenta()),
                () -> assertEquals("1234567890123456789012", result.cbu()),
                () -> assertEquals("aliasTest", result.alias()),
                () -> assertEquals(1, result.idBanco()));
    }

    @Test
    void updateEntityFromRequest_DeberiaActualizarCampos_CuandoRequestEsValido() {
        CuentaBancaria cuentaBancaria = CuentaBancaria.builder()
                .id(1)
                .numeroCuenta("123456789")
                .banco(Banco.builder().id(1).nombre("Banco Viejo").build())
                .build();
        CuentaRequest request = CuentaRequest.builder()
                .idBanco(2)
                .numeroCuenta("987654321")
                .build();
        cuentaMapper.updateEntityFromRequest(request, cuentaBancaria);

        assertAll("Verificaciones de actualización de CuentaBancaria",
                () -> assertNotNull(cuentaBancaria),
                () -> assertEquals(1, cuentaBancaria.getId()),
                () -> assertEquals("987654321", cuentaBancaria.getNumeroCuenta()),
                () -> assertNotNull(cuentaBancaria.getBanco()),
                () -> assertEquals(2, cuentaBancaria.getBanco().getId()),
                () -> assertEquals("Banco Viejo", cuentaBancaria.getBanco().getNombre()));
    }
}
