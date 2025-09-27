package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.TarjetaCreditoRepository;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.mapper.TarjetaCreditoMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TarjetaCreditoServiceTest {

  @Mock private TarjetaCreditoRepository tarjetaCreditoRepository;

  @Mock private TarjetaCreditoMapper tarjetaCreditoMapper;

  @InjectMocks private TarjetaCreditoService tarjetaCreditoService;

  private TarjetaCredito tarjeta;
  private TarjetaCreditoRequest request;
  private TarjetaCreditoResponse response;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    Huesped huesped = Huesped.builder().id(10).build();
    tarjeta =
        TarjetaCredito.builder()
            .id(1)
            .numero("4111111111111111")
            .nombreTitular("Juan Perez")
            .cvc("123")
            .fechaVencimiento("12/25")
            .esPrincipal(false)
            .huesped(huesped)
            .build();
    request =
        TarjetaCreditoRequest.builder()
            .numero("4111111111111111")
            .nombreTitular("Juan Perez")
            .cvc("123")
            .fechaVencimiento("12/25")
            .esPrincipal(false)
            .idBanco(1)
            .build();
    response =
        TarjetaCreditoResponse.builder()
            .id(1)
            .numero("4111111111111111")
            .nombreTitular("Juan Perez")
            .cvc("123")
            .fechaVencimiento("12/25")
            .esPrincipal(false)
            .idBanco(1)
            .build();
  }

  @Test
  void getTarjetaById_DeberiaRetornar200_CuandoExiste() {
    when(tarjetaCreditoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
    when(tarjetaCreditoMapper.toResponse(tarjeta)).thenReturn(response);

    TarjetaCreditoResponse result = tarjetaCreditoService.getTarjetaById(1L);

    assertEquals(1, result.id());
    assertEquals("4111111111111111", result.numero());
  }

  @Test
  void getTarjetaById_DeberiaRetornar404_CuandoNoExiste() {
    when(tarjetaCreditoRepository.findById(2L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> tarjetaCreditoService.getTarjetaById(2L));
  }

  @Test
  void crearTarjeta_DeberiaRetornar201_CuandoEsValida() {
    when(tarjetaCreditoMapper.toEntity(request)).thenReturn(tarjeta);
    when(tarjetaCreditoRepository.save(tarjeta)).thenReturn(tarjeta);
    when(tarjetaCreditoMapper.toResponse(tarjeta)).thenReturn(response);

    TarjetaCreditoResponse result = tarjetaCreditoService.crearTarjeta(request);

    assertEquals("Juan Perez", result.nombreTitular());
  }

  @Test
  void crearTarjeta_DeberiaRetornar400_CuandoEsNull() {
    assertThrows(IllegalArgumentException.class, () -> tarjetaCreditoService.crearTarjeta(null));
  }

  @Test
  void updateTarjetaCredito_DeberiaRetornar200_CuandoEsValida() {
    when(tarjetaCreditoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
    doAnswer(
            invocation -> {
              // simulate mapper updating the entity
              TarjetaCreditoRequest req = invocation.getArgument(0);
              TarjetaCredito t = invocation.getArgument(1);
              t.setNombreTitular(req.nombreTitular());
              return null;
            })
        .when(tarjetaCreditoMapper)
        .updateEntityFromRequest(eq(request), any(TarjetaCredito.class));
    when(tarjetaCreditoRepository.save(tarjeta)).thenReturn(tarjeta);
    when(tarjetaCreditoMapper.toResponse(tarjeta)).thenReturn(response);

    TarjetaCreditoResponse result = tarjetaCreditoService.updateTarjetaCredito(1L, request);
    assertEquals(1, result.id());
  }

  @Test
  void updateTarjetaCredito_DeberiaRetornar400_CuandoParametrosInvalidos() {
    assertThrows(
        IllegalArgumentException.class,
        () -> tarjetaCreditoService.updateTarjetaCredito(null, request));
    assertThrows(
        IllegalArgumentException.class, () -> tarjetaCreditoService.updateTarjetaCredito(1L, null));
  }

  @Test
  void deleteTarjetaCredito_DeberiaEliminar_CuandoNoEsPrincipal() {
    when(tarjetaCreditoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
    doNothing().when(tarjetaCreditoRepository).delete(tarjeta);
    assertDoesNotThrow(() -> tarjetaCreditoService.deleteTarjetaCredito(1L));
    verify(tarjetaCreditoRepository, times(1)).delete(tarjeta);
  }

  @Test
  void deleteTarjetaCredito_DeberiaFallar_CuandoEsPrincipal() {
    tarjeta.setEsPrincipal(true);
    when(tarjetaCreditoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
    assertThrows(IllegalStateException.class, () -> tarjetaCreditoService.deleteTarjetaCredito(1L));
    verify(tarjetaCreditoRepository, never()).delete(any());
  }

  @Test
  void setTarjetaPrincipal_DeberiaMarcarYDesactivarOtras() {
    // tarjeta actual
    TarjetaCredito otra1 =
        TarjetaCredito.builder().id(2).huesped(tarjeta.getHuesped()).esPrincipal(true).build();
    TarjetaCredito otra2 =
        TarjetaCredito.builder().id(3).huesped(tarjeta.getHuesped()).esPrincipal(true).build();

    when(tarjetaCreditoRepository.findById(1L)).thenReturn(Optional.of(tarjeta));
    when(tarjetaCreditoRepository.findByHuespedIdAndIdNot(tarjeta.getHuesped().getId(), 1L))
        .thenReturn(List.of(otra1, otra2));
    when(tarjetaCreditoRepository.saveAll(anyList())).thenReturn(List.of(otra1, otra2));
    when(tarjetaCreditoRepository.save(tarjeta)).thenReturn(tarjeta);
    when(tarjetaCreditoMapper.toResponse(tarjeta)).thenReturn(response);

    TarjetaCreditoResponse result = tarjetaCreditoService.setTarjetaPrincipal(1L);
    assertEquals(1, result.id());
    assertFalse(otra1.isPrincipal());
    assertFalse(otra2.isPrincipal());
  }
}
