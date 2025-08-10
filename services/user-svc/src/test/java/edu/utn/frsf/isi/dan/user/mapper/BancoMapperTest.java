package edu.utn.frsf.isi.dan.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;

import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.model.Banco;

public class BancoMapperTest {

    private BancoMapper bancoMapper;

    @BeforeEach
    void setUp() {
        bancoMapper = Mappers.getMapper(BancoMapper.class);
    }

    @Test
    void toEntity_DeberiaRetornarEntidad_CuandoRequestEsValido() {
        BancoRequest request = BancoRequest.builder()
                .nombre("Banco Test")
                .build();

        Banco banco = bancoMapper.toEntity(request);

        assertNotNull(banco);
        assertNull(banco.getId());
        assertEquals("Banco Test", banco.getNombre());
    }

    @Test
    void toResponse_DeberiaRetornarDTO_CuandoEntidadEsValida() {
        Banco banco = Banco.builder()
                .id(1)
                .nombre("Banco Test")
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .fechaEliminado(null)
                .fechaModificado(null)
                .build();

        BancoResponse result = bancoMapper.toResponse(banco);

        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("Banco Test", result.nombre());
        assertTrue(result.activo());
    }

    @Test
    void updateEntityFromRequest_DeberiaActualizarCampos_CuandoRequestEsValido() {
        Banco banco = Banco.builder()
                .id(1)
                .nombre("Banco Viejo")
                .activo(true)
                .build();

        BancoRequest request = BancoRequest.builder()
                .nombre("Banco Nuevo")
                .build();

        bancoMapper.updateEntityFromRequest(request, banco);

        assertEquals("Banco Nuevo", banco.getNombre());
        assertEquals(1, banco.getId());
    }

}
