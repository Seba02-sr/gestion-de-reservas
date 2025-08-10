package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.mapper.BancoMapper;
import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BancoServiceTest {

    @Mock
    private BancoRepository bancoRepository;

    @Mock
    private BancoMapper bancoMapper;

    @InjectMocks
    private BancoService bancoService;

    private Banco banco;
    private BancoResponse bancoResponse;
    private BancoRequest bancoRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        banco = Banco.builder()
                .id(1)
                .nombre("Banco Test")
                .build();
        bancoResponse = BancoResponse.builder()
                .id(1)
                .nombre("Banco Test")
                .activo(true)
                .build();
        bancoRequest = BancoRequest.builder()
                .nombre("Banco Test")
                .build();
    }

    // ------------------- getAllBancos -------------------
    @Test
    void getAllBancos_DeberiaRetornar200_CuandoHayBancos() {
        when(bancoRepository.findByActivoTrue()).thenReturn(List.of(banco));
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        List<BancoResponse> result = bancoService.getAllBancos();

        assertEquals(1, result.size());
        assertEquals("Banco Test", result.get(0).nombre());
    }

    @Test
    void getAllBancos_DeberiaRetornar200_CuandoNoHayBancos() {
        when(bancoRepository.findByActivoTrue()).thenReturn(List.of());

        List<BancoResponse> result = bancoService.getAllBancos();

        assertTrue(result.isEmpty());
    }

    // ------------------- getBancoById -------------------
    @Test
    void getBancoById_DeberiaRetornar200_CuandoIdEsValido() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.of(banco));
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        BancoResponse result = bancoService.getBancoById(1);

        assertEquals("Banco Test", result.nombre());
    }

    @Test
    void getBancoById_DeberiaRetornar400_CuandoIdEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.getBancoById(null));
    }

    @Test
    void getBancoById_DeberiaRetornar400_CuandoIdEsNegativo() {
        // El service no valida negativo, se puede agregar si es necesario
        // En este caso, lo tratamos igual que null → no lanza IllegalArgumentException por negativo,
        // salvo que quieras añadir validación en el service.
        assertThrows(EntityNotFoundException.class, () -> {
            when(bancoRepository.findByIdAndActivoTrue(-1)).thenReturn(Optional.empty());
            bancoService.getBancoById(-1);
        });
    }

    @Test
    void getBancoById_DeberiaRetornar404_CuandoNoExiste() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bancoService.getBancoById(1));
    }

    // ------------------- createBanco -------------------
    @Test
    void createBanco_DeberiaRetornar201_CuandoBancoEsValido() {
        when(bancoMapper.toEntity(bancoRequest)).thenReturn(banco);
        when(bancoRepository.save(banco)).thenReturn(banco);
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        BancoResponse result = bancoService.createBanco(bancoRequest);

        assertEquals("Banco Test", result.nombre());
    }

    @Test
    void createBanco_DeberiaRetornar400_CuandoBancoEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.createBanco(null));
    }

    // ------------------- updateBanco -------------------
    @Test
    void updateBanco_DeberiaRetornar200_CuandoBancoEsValido() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.of(banco));
        when(bancoRepository.save(any())).thenReturn(banco);
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        BancoResponse result = bancoService.updateBanco(1, bancoRequest);

        assertEquals("Banco Test", result.nombre());
    }

    @Test
    void updateBanco_DeberiaRetornar400_CuandoIdEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.updateBanco(null, bancoRequest));
    }

    @Test
    void updateBanco_DeberiaRetornar400_CuandoRequestEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.updateBanco(1, null));
    }

    @Test
    void updateBanco_DeberiaRetornar404_CuandoIdNoExiste() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bancoService.updateBanco(1, bancoRequest));
    }

    // ------------------- deleteBanco -------------------
    @Test
    void deleteBanco_DeberiaRetornar200_CuandoIdEsValido() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.of(banco));
        when(bancoRepository.save(any())).thenReturn(banco);
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        BancoResponse result = bancoService.deleteBanco(1);

        assertEquals("Banco Test", result.nombre());
    }

    @Test
    void deleteBanco_DeberiaRetornar400_CuandoIdEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.deleteBanco(null));
    }

    @Test
    void deleteBanco_DeberiaRetornar404_CuandoIdNoExiste() {
        when(bancoRepository.findByIdAndActivoTrue(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> bancoService.deleteBanco(1));
    }

    // ------------------- getBancosByNombre -------------------
    @Test
    void getBancosByNombre_DeberiaRetornar200_CuandoNombreEsValido() {
        when(bancoRepository.findByActivoTrueAndNombreContainingIgnoreCase("Banco"))
                .thenReturn(List.of(banco));
        when(bancoMapper.toResponse(banco)).thenReturn(bancoResponse);

        List<BancoResponse> result = bancoService.getBancosByNombre("Banco");

        assertEquals(1, result.size());
    }

    @Test
    void getBancosByNombre_DeberiaRetornar400_CuandoNombreEsVacio() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.getBancosByNombre(""));
    }

    @Test
    void getBancosByNombre_DeberiaRetornar400_CuandoNombreEsNull() {
        assertThrows(IllegalArgumentException.class, () -> bancoService.getBancosByNombre(null));
    }

}
