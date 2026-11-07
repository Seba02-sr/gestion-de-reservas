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

  @Mock private TarjetaCreditoRepository repo;
  @Mock private TarjetaCreditoMapper mapper;
  @Mock private HuespedService huespedService;
  @InjectMocks private TarjetaCreditoService service;

  private TarjetaCredito principal;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    principal =
        TarjetaCredito.builder()
            .id(1)
            .esPrincipal(true)
            .huesped(Huesped.builder().id(9).build())
            .build();
  }

  @Test
  void getTarjetaById_NotFound_Throws404() {
    when(repo.findById(99)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> service.getTarjetaById(99));
  }

  @Test
  void deleteTarjeta_Principal_ShouldThrow409() {
    when(repo.findById(1)).thenReturn(Optional.of(principal));
    assertThrows(IllegalStateException.class, () -> service.deleteTarjetaCredito(1));
    verify(repo, never()).delete(any());
  }

  @Test
  void setTarjetaPrincipal_DisablesOthersAndEnablesTarget() {
    TarjetaCredito otra1 =
        TarjetaCredito.builder().id(2).esPrincipal(true).huesped(principal.getHuesped()).build();
    TarjetaCredito otra2 =
        TarjetaCredito.builder().id(3).esPrincipal(false).huesped(principal.getHuesped()).build();
    when(repo.findById(1)).thenReturn(Optional.of(principal));
    when(repo.findByHuespedIdAndIdNot(9, 1)).thenReturn(List.of(otra1, otra2));
    when(repo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    when(repo.save(any(TarjetaCredito.class))).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(TarjetaCredito.class)))
        .thenAnswer(
            inv -> {
              TarjetaCredito t = inv.getArgument(0);
              return TarjetaCreditoResponse.builder()
                  .id(t.getId())
                  .esPrincipal(t.getEsPrincipal())
                  .build();
            });

    TarjetaCreditoResponse resp = service.setTarjetaPrincipal(1);
    assertTrue(resp.esPrincipal());
    assertFalse(otra1.getEsPrincipal());
    assertFalse(otra2.getEsPrincipal());
  }

  private TarjetaCreditoRequest requestTarjeta(boolean principal) {
    return TarjetaCreditoRequest.builder()
        .numero("4111111111111111")
        .nombreTitular("Juan")
        .fechaVencimiento("12/25")
        .cvc("123")
        .esPrincipal(principal)
        .idBanco(1)
        .build();
  }

  private void stubMapperYSave() {
    when(repo.save(any(TarjetaCredito.class))).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(TarjetaCredito.class)))
        .thenAnswer(
            inv -> {
              TarjetaCredito t = inv.getArgument(0);
              return TarjetaCreditoResponse.builder()
                  .id(t.getId())
                  .esPrincipal(t.getEsPrincipal())
                  .build();
            });
  }

  @Test
  void crearTarjetaParaHuesped_ConPrincipal_DesmarcaLaAnterior() {
    Huesped huesped = principal.getHuesped();
    TarjetaCredito nueva = TarjetaCredito.builder().esPrincipal(true).build();
    when(huespedService.getHuespedEntityById(9)).thenReturn(huesped);
    when(mapper.toEntity(any(TarjetaCreditoRequest.class))).thenReturn(nueva);
    when(repo.findByHuespedId(9)).thenReturn(List.of(principal));
    when(repo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    stubMapperYSave();

    TarjetaCreditoResponse resp = service.crearTarjetaParaHuesped(9, requestTarjeta(true));

    assertTrue(resp.esPrincipal());
    assertFalse(principal.getEsPrincipal());
    assertEquals(huesped, nueva.getHuesped());
  }

  @Test
  void crearTarjetaParaHuesped_SinPrincipal_NoTocaLasDemas() {
    Huesped huesped = principal.getHuesped();
    TarjetaCredito nueva = TarjetaCredito.builder().esPrincipal(false).build();
    when(huespedService.getHuespedEntityById(9)).thenReturn(huesped);
    when(mapper.toEntity(any(TarjetaCreditoRequest.class))).thenReturn(nueva);
    stubMapperYSave();

    TarjetaCreditoResponse resp = service.crearTarjetaParaHuesped(9, requestTarjeta(false));

    assertFalse(resp.esPrincipal());
    assertTrue(principal.getEsPrincipal());
    verify(repo, never()).saveAll(anyList());
  }

  @Test
  void deleteTarjeta_NoPrincipal_Elimina() {
    TarjetaCredito comun =
        TarjetaCredito.builder().id(2).esPrincipal(false).huesped(principal.getHuesped()).build();
    when(repo.findById(2)).thenReturn(Optional.of(comun));

    service.deleteTarjetaCredito(2);

    verify(repo).delete(comun);
  }

  @Test
  void updateTarjeta_PrincipalConEsPrincipalFalse_Throws409() {
    when(repo.findById(1)).thenReturn(Optional.of(principal));

    assertThrows(
        IllegalStateException.class, () -> service.updateTarjetaCredito(1, requestTarjeta(false)));
    verify(repo, never()).save(any());
  }

  @Test
  void updateTarjeta_NoPrincipalConEsPrincipalTrue_DesmarcaLasOtras() {
    TarjetaCredito comun =
        TarjetaCredito.builder().id(2).esPrincipal(false).huesped(principal.getHuesped()).build();
    when(repo.findById(2)).thenReturn(Optional.of(comun));
    when(repo.findByHuespedIdAndIdNot(9, 2)).thenReturn(List.of(principal));
    when(repo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    doAnswer(
            inv -> {
              TarjetaCreditoRequest r = inv.getArgument(0);
              TarjetaCredito t = inv.getArgument(1);
              t.setEsPrincipal(r.esPrincipal());
              return null;
            })
        .when(mapper)
        .updateEntityFromRequest(any(TarjetaCreditoRequest.class), any(TarjetaCredito.class));
    stubMapperYSave();

    TarjetaCreditoResponse resp = service.updateTarjetaCredito(2, requestTarjeta(true));

    assertTrue(resp.esPrincipal());
    assertFalse(principal.getEsPrincipal());
  }
}
