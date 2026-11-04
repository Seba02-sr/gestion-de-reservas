package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.TarifaMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.TarifaMapperImpl;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/** Todas las pruebas usan "hoy" = 2026-11-04 (reloj fijo) y fechas concretas. */
@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

  @Mock private TarifaRepository tarifaRepository;
  @Mock private TipoHabitacionRepository tipoHabitacionRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Spy private TarifaMapper tarifaMapper = new TarifaMapperImpl();

  @InjectMocks private TarifaService tarifaService;

  private final TipoHabitacion tipo = new TipoHabitacion(3, "DOBLE", "Doble una cama", 2);
  private final AtomicInteger ids = new AtomicInteger(100);
  private List<Tarifa> enBase;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        tarifaService, "clock", Clock.fixed(HOY.atTime(12, 0).atZone(ZONA).toInstant(), ZONA));
    enBase = new ArrayList<>();
  }

  // ------------------------------------------------------------------ helpers

  private Tarifa tarifa(
      int id, String nombre, LocalDate inicio, LocalDate fin, double precio, boolean especial) {
    return Tarifa.builder()
        .id(id)
        .nombre(nombre)
        .tipoHabitacion(tipo)
        .fechaInicio(inicio)
        .fechaFin(fin)
        .precioNoche(precio)
        .especial(especial)
        .build();
  }

  /** Deja las tarifas "en la base" del tipo 3 y prepara guardar/borrar. */
  private void conTarifas(Tarifa... tarifas) {
    enBase = new ArrayList<>(List.of(tarifas));
    lenient().when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    lenient()
        .when(tarifaRepository.findByTipoHabitacionIdOrderByFechaInicioAsc(3))
        .thenAnswer(inv -> new ArrayList<>(enBase));
    lenient()
        .when(tarifaRepository.save(any(Tarifa.class)))
        .thenAnswer(
            inv -> {
              Tarifa t = inv.getArgument(0);
              if (t.getId() == null) {
                t.setId(ids.incrementAndGet());
              }
              if (enBase.stream().noneMatch(x -> x == t)) {
                enBase.add(t);
              }
              return t;
            });
    lenient()
        .doAnswer(
            inv -> {
              Tarifa t = inv.getArgument(0);
              enBase.removeIf(x -> x == t);
              return null;
            })
        .when(tarifaRepository)
        .delete(any(Tarifa.class));
    for (Tarifa t : tarifas) {
      lenient().when(tarifaRepository.findById(t.getId())).thenReturn(Optional.of(t));
    }
  }

  private TarifaRequest comun(double precio) {
    return TarifaRequest.builder()
        .nombre("Tarifa " + precio)
        .idTipoHabitacion(3)
        .precioNoche(precio)
        .build();
  }

  private TarifaRequest promo(LocalDate inicio, LocalDate fin, double precio) {
    return TarifaRequest.builder()
        .nombre("Promo")
        .idTipoHabitacion(3)
        .precioNoche(precio)
        .fechaInicio(inicio)
        .fechaFin(fin)
        .build();
  }

  private Tarifa enBase(int id) {
    return enBase.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
  }

  private List<Tarifa> ordenadas() {
    return enBase.stream().sorted(java.util.Comparator.comparing(Tarifa::getFechaInicio)).toList();
  }

  private List<HabitacionEvent> eventos(int cantidad) {
    ArgumentCaptor<HabitacionEventoPublicado> captor =
        ArgumentCaptor.forClass(HabitacionEventoPublicado.class);
    verify(eventPublisher, times(cantidad)).publishEvent(captor.capture());
    return captor.getAllValues().stream().map(HabitacionEventoPublicado::evento).toList();
  }

  private static void assertEventoPrecio(HabitacionEvent e, int tipoId, double precio) {
    assertEquals(TipoEvento.ACTUALIZAR_PRECIO, e.getTipoEvento());
    assertEquals(tipoId, e.getTarifa().getTipoHabitacionId());
    assertEquals(precio, e.getTarifa().getNuevoPrecio());
  }

  // ------------------------------------------------- vigente (inicio <= hoy, fin null o >= hoy)

  @Test
  void obtenerTarifaVigente_consultaElRepositorioConLaFechaPedida() {
    Tarifa t = tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false);
    when(tarifaRepository.findVigentes(3, LocalDate.of(2026, 12, 25))).thenReturn(List.of(t));

    assertEquals(Optional.of(t), tarifaService.obtenerTarifaVigente(3, LocalDate.of(2026, 12, 25)));
  }

  @Test
  void obtenerTarifaVigente_sinFechaUsaHoy() {
    when(tarifaRepository.findVigentes(3, HOY)).thenReturn(List.of());

    assertTrue(tarifaService.obtenerTarifaVigente(3).isEmpty());
    verify(tarifaRepository).findVigentes(3, HOY);
  }

  @Test
  void vigente_sinTarifaLanzaEntityNotFound() {
    when(tarifaRepository.findVigentes(3, HOY)).thenReturn(List.of());

    assertThrows(EntityNotFoundException.class, () -> tarifaService.vigente(3, null));
  }

  // ---------------------------------------------- regla 2: tarifa nueva sin fechas

  @Test
  void crearSinFechas_tipoSinTarifas_empiezaHoyYSinFin() {
    conTarifas();

    TarifaResponse r = tarifaService.crear(comun(10000));

    assertEquals(HOY, r.fechaInicio());
    assertNull(r.fechaFin());
    assertEquals(10000.0, r.precioNoche());
    assertFalse(r.especial());
    assertEquals(1, enBase.size());
    assertEventoPrecio(eventos(1).get(0), 3, 10000.0);
  }

  @Test
  void crearSinFechas_conTarifaActual_laActualTerminaAyerYLaNuevaEmpiezaHoy() {
    conTarifas(tarifa(1, "Base 2026", LocalDate.of(2026, 1, 1), null, 10000, false));

    TarifaResponse r = tarifaService.crear(comun(12000));

    Tarifa vieja = enBase(1);
    assertEquals(LocalDate.of(2026, 1, 1), vieja.getFechaInicio());
    assertEquals(LocalDate.of(2026, 11, 3), vieja.getFechaFin()); // ayer
    assertEquals(HOY, r.fechaInicio());
    assertNull(r.fechaFin());
    assertEquals(12000.0, r.precioNoche());
    assertEquals(2, enBase.size());
    assertEventoPrecio(eventos(1).get(0), 3, 12000.0);
  }

  @Test
  void crearSinFechas_conElMismoPrecio_noPublicaElEvento() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    tarifaService.crear(comun(10000));

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void crearSinFechas_siLaActualEmpezoHoy_laReemplazaEnLugarDeDejarlaConFinMenorQueInicio() {
    conTarifas(tarifa(1, "Creada hoy", HOY, null, 10000, false));

    tarifaService.crear(comun(11000));

    assertNull(enBase(1)); // se eliminó
    assertEquals(1, enBase.size());
    assertEquals(11000.0, enBase.get(0).getPrecioNoche());
    assertEquals(HOY, enBase.get(0).getFechaInicio());
  }

  @Test
  void
      crearSinFechas_conPromoFutura_laNuevaRigeHastaElDiaAnteriorALaPromoYLaPosteriorPasaAlNuevoPrecio() {
    conTarifas(
        tarifa(1, "Base 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base 2026", LocalDate.of(2027, 1, 6), null, 10000, false));

    TarifaResponse r = tarifaService.crear(comun(12000));

    List<Tarifa> t = ordenadas();
    assertEquals(4, t.size());
    // la base anterior termina ayer
    assertEquals(LocalDate.of(2026, 1, 1), t.get(0).getFechaInicio());
    assertEquals(LocalDate.of(2026, 11, 3), t.get(0).getFechaFin());
    // la nueva: desde hoy hasta el día anterior a la próxima promo
    assertEquals(HOY, t.get(1).getFechaInicio());
    assertEquals(LocalDate.of(2026, 12, 19), t.get(1).getFechaFin());
    assertEquals(12000.0, t.get(1).getPrecioNoche());
    assertFalse(t.get(1).getEspecial());
    assertEquals(t.get(1).getId(), r.id());
    assertEquals(LocalDate.of(2026, 12, 19), r.fechaFin());
    // la promo no se toca
    assertEquals(LocalDate.of(2026, 12, 20), t.get(2).getFechaInicio());
    assertEquals(LocalDate.of(2027, 1, 5), t.get(2).getFechaFin());
    assertEquals(15000.0, t.get(2).getPrecioNoche());
    assertTrue(t.get(2).getEspecial());
    // la tarifa posterior a la promo sigue ahí, abierta, pero con el precio nuevo
    assertEquals(LocalDate.of(2027, 1, 6), t.get(3).getFechaInicio());
    assertNull(t.get(3).getFechaFin());
    assertEquals(12000.0, t.get(3).getPrecioNoche());
    // el precio de hoy cambió (10000 -> 12000)
    assertEventoPrecio(eventos(1).get(0), 3, 12000.0);
  }

  @Test
  void crearSinFechas_conVariasPromosFuturas_repreciaTodasLasTarifasPosterioresAUnaPromo() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo 1", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), LocalDate.of(2027, 2, 28), 10000, false),
        tarifa(4, "Promo 2", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 10), 9000, true),
        tarifa(5, "Base", LocalDate.of(2027, 3, 11), null, 10000, false));

    tarifaService.crear(comun(12000));

    List<Tarifa> t = ordenadas();
    assertEquals(6, t.size());
    assertEquals(
        List.of(10000.0, 12000.0, 15000.0, 12000.0, 9000.0, 12000.0),
        t.stream().map(Tarifa::getPrecioNoche).toList());
    // la nueva solo llega hasta la primera promo
    assertEquals(LocalDate.of(2026, 12, 19), t.get(1).getFechaFin());
    // las fechas de las posteriores no cambian
    assertEquals(LocalDate.of(2027, 2, 28), t.get(3).getFechaFin());
    assertNull(t.get(5).getFechaFin());
    // ninguna promo cambió
    assertTrue(t.get(2).getEspecial());
    assertTrue(t.get(4).getEspecial());
  }

  @Test
  void crearSinFechas_conUnaPromoVigenteHoy_laCortaAyerYAbsorbeSuContinuacion() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 31), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), 8000, true),
        tarifa(3, "Base", LocalDate.of(2026, 11, 11), null, 10000, false));

    tarifaService.crear(comun(12000));

    List<Tarifa> t = ordenadas();
    assertEquals(3, t.size());
    assertEquals(LocalDate.of(2026, 11, 3), t.get(1).getFechaFin()); // la promo termina ayer
    assertEquals(HOY, t.get(2).getFechaInicio());
    assertNull(t.get(2).getFechaFin());
    assertEquals(12000.0, t.get(2).getPrecioNoche());
    assertNull(enBase(3)); // la continuación de la promo quedó absorbida por la nueva
    assertEventoPrecio(eventos(1).get(0), 3, 12000.0);
  }

  @Test
  void crearSinFechas_conUnaTarifaFuturaSinPromo_laNuevaLaAbsorbe() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 30), 10000, false),
        tarifa(2, "Futura", LocalDate.of(2026, 12, 1), null, 11000, false));

    tarifaService.crear(comun(12000));

    List<Tarifa> t = ordenadas();
    assertEquals(2, t.size());
    assertEquals(LocalDate.of(2026, 11, 3), t.get(0).getFechaFin());
    assertEquals(HOY, t.get(1).getFechaInicio());
    assertNull(t.get(1).getFechaFin());
  }

  @Test
  void crearSinFechas_conPromoFutura_conElMismoPrecioDeHoyNoPublicaEvento() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), null, 10000, false));

    tarifaService.crear(comun(10000));

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void crearSinFechas_tipoInexistenteLanzaEntityNotFound() {
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> tarifaService.crear(comun(10000)));
  }

  // ----------------------------------------- regla 3: tarifa especial / promocional

  @Test
  void crearPromo_enElMedio_cortaLaActualCreaLaPromoYUnaContinuacionConElMismoPrecio() {
    conTarifas(tarifa(1, "Base 2026", LocalDate.of(2026, 1, 1), null, 10000, false));

    TarifaResponse r =
        tarifaService.crear(promo(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000));

    List<Tarifa> t = ordenadas();
    assertEquals(3, t.size());
    // a. la actual queda con fin = día anterior al inicio de la promo
    assertEquals(LocalDate.of(2026, 1, 1), t.get(0).getFechaInicio());
    assertEquals(LocalDate.of(2026, 12, 19), t.get(0).getFechaFin());
    assertEquals(10000.0, t.get(0).getPrecioNoche());
    // b. la promo, con su inicio y fin
    assertEquals(LocalDate.of(2026, 12, 20), t.get(1).getFechaInicio());
    assertEquals(LocalDate.of(2027, 1, 5), t.get(1).getFechaFin());
    assertEquals(15000.0, t.get(1).getPrecioNoche());
    assertTrue(t.get(1).getEspecial());
    assertEquals(t.get(1).getId(), r.id());
    // c. continuación: día siguiente al fin de la promo, sin fin, mismo precio que la cortada
    assertEquals(LocalDate.of(2027, 1, 6), t.get(2).getFechaInicio());
    assertNull(t.get(2).getFechaFin());
    assertEquals(10000.0, t.get(2).getPrecioNoche());
    assertEquals("Base 2026", t.get(2).getNombre());
    assertFalse(t.get(2).getEspecial());
  }

  @Test
  void crearPromo_futura_noCambiaElPrecioDeHoyAsiQueNoPublicaEvento() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    tarifaService.crear(promo(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000));

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void crearPromo_queEmpiezaHoy_publicaElNuevoPrecioAhora() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    tarifaService.crear(promo(HOY, LocalDate.of(2026, 11, 10), 8000));

    assertEventoPrecio(eventos(1).get(0), 3, 8000.0);
    Tarifa base = enBase(1);
    assertEquals(LocalDate.of(2026, 11, 3), base.getFechaFin());
  }

  @Test
  void crearPromo_queEmpiezaElMismoDiaQueLaBase_laBaseSeReemplazaPorLaContinuacion() {
    conTarifas(tarifa(1, "Base", HOY, null, 10000, false));

    tarifaService.crear(promo(HOY, LocalDate.of(2026, 11, 10), 8000));

    List<Tarifa> t = ordenadas();
    assertEquals(2, t.size());
    assertEquals(8000.0, t.get(0).getPrecioNoche());
    assertEquals(LocalDate.of(2026, 11, 11), t.get(1).getFechaInicio());
    assertEquals(10000.0, t.get(1).getPrecioNoche());
    assertNull(t.get(1).getFechaFin());
  }

  @Test
  void crearPromo_conInicioPosteriorAlFin_lanzaIllegalArgument() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tarifaService.crear(
                promo(LocalDate.of(2027, 1, 5), LocalDate.of(2026, 12, 20), 15000)));
  }

  @Test
  void crearPromo_conInicioIgualAlFin_esValida() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    tarifaService.crear(promo(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 24), 20000));

    List<Tarifa> t = ordenadas();
    assertEquals(LocalDate.of(2026, 12, 23), t.get(0).getFechaFin());
    assertEquals(LocalDate.of(2026, 12, 24), t.get(1).getFechaInicio());
    assertEquals(LocalDate.of(2026, 12, 24), t.get(1).getFechaFin());
    assertEquals(LocalDate.of(2026, 12, 25), t.get(2).getFechaInicio());
  }

  @Test
  void crearPromo_conInicioEnElPasado_lanzaIllegalArgument() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tarifaService.crear(promo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8), 9000)));
  }

  @Test
  void crearPromo_conUnaSolaFecha_lanzaIllegalArgument() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));
    TarifaRequest soloInicio =
        TarifaRequest.builder()
            .nombre("Promo")
            .idTipoHabitacion(3)
            .precioNoche(9000.0)
            .fechaInicio(LocalDate.of(2026, 12, 1))
            .build();

    assertThrows(IllegalArgumentException.class, () -> tarifaService.crear(soloInicio));
  }

  @Test
  void crearPromo_quePisaOtraPromo_lanzaIllegalState() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), null, 10000, false));

    // se superpone al principio, al final, por adentro y por afuera
    for (LocalDate[] f :
        new LocalDate[][] {
          {LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 20)},
          {LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 10)},
          {LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 26)},
          {LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 1)}
        }) {
      assertThrows(
          IllegalStateException.class,
          () -> tarifaService.crear(promo(f[0], f[1], 9000)),
          f[0] + " a " + f[1]);
    }
    verify(tarifaRepository, never()).save(any());
  }

  @Test
  void crearPromo_justoDespuesDeOtraPromo_esValida() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), null, 10000, false));

    tarifaService.crear(promo(LocalDate.of(2027, 1, 6), LocalDate.of(2027, 1, 10), 9000));

    List<Tarifa> t = ordenadas();
    assertEquals(4, t.size());
    assertEquals(LocalDate.of(2027, 1, 6), t.get(2).getFechaInicio());
    assertEquals(LocalDate.of(2027, 1, 10), t.get(2).getFechaFin());
    assertEquals(9000.0, t.get(2).getPrecioNoche());
    assertEquals(LocalDate.of(2027, 1, 11), t.get(3).getFechaInicio());
    assertNull(t.get(3).getFechaFin());
    assertEquals(10000.0, t.get(3).getPrecioNoche());
  }

  @Test
  void crearPromo_antesDeUnaPromoYaProgramada_esValidaYRespetaElFinOriginalDeLaBase() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), null, 10000, false));

    tarifaService.crear(promo(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 9000));

    List<Tarifa> t = ordenadas();
    assertEquals(5, t.size());
    assertEquals(LocalDate.of(2026, 11, 30), t.get(0).getFechaFin());
    assertEquals(LocalDate.of(2026, 12, 1), t.get(1).getFechaInicio());
    // la continuación hereda el fin original de la base (19/12), no queda abierta
    assertEquals(LocalDate.of(2026, 12, 6), t.get(2).getFechaInicio());
    assertEquals(LocalDate.of(2026, 12, 19), t.get(2).getFechaFin());
    assertEquals(LocalDate.of(2026, 12, 20), t.get(3).getFechaInicio());
  }

  @Test
  void crearPromo_sinTarifaBase_lanzaIllegalState() {
    conTarifas();

    assertThrows(
        IllegalStateException.class,
        () ->
            tarifaService.crear(
                promo(LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000)));
  }

  // ------------------------------------------------------------- regla 4: DELETE

  @Test
  void eliminar_laUnicaTarifa_lanzaIllegalStateYNoBorra() {
    conTarifas(tarifa(1, "Base", LocalDate.of(2026, 1, 1), null, 10000, false));

    assertThrows(IllegalStateException.class, () -> tarifaService.eliminar(1));
    verify(tarifaRepository, never()).delete(any());
  }

  @Test
  void eliminar_inexistente_lanzaEntityNotFound() {
    when(tarifaRepository.findById(99)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> tarifaService.eliminar(99));
  }

  @Test
  void eliminar_laVigente_laAnteriorExtiendeSuFinYPasaASerVigente() {
    conTarifas(
        tarifa(1, "Base 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 3), 10000, false),
        tarifa(2, "Nueva", HOY, null, 12000, false));

    tarifaService.eliminar(2);

    assertEquals(1, enBase.size());
    assertNull(enBase(1).getFechaFin()); // ahora abierta: vigente hoy, sin hueco
    assertEventoPrecio(eventos(1).get(0), 3, 10000.0);
  }

  @Test
  void eliminar_laVigenteQueEsUnaPromo_laAnteriorCubreHastaElFinDeLaPromo() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 3), 10000, false),
        tarifa(2, "Promo", HOY, LocalDate.of(2026, 11, 10), 8000, true),
        tarifa(3, "Base", LocalDate.of(2026, 11, 11), null, 10000, false));

    tarifaService.eliminar(2);

    List<Tarifa> t = ordenadas();
    assertEquals(2, t.size());
    assertEquals(LocalDate.of(2026, 11, 10), t.get(0).getFechaFin()); // pegada con la siguiente
    assertEquals(LocalDate.of(2026, 11, 11), t.get(1).getFechaInicio());
    assertEventoPrecio(eventos(1).get(0), 3, 10000.0);
  }

  @Test
  void eliminar_laVigenteSinTarifaAnterior_lanzaIllegalState() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true));

    assertThrows(IllegalStateException.class, () -> tarifaService.eliminar(1));
    assertEquals(2, enBase.size());
  }

  @Test
  void eliminar_unaNoVigentePasada_seBorraDirectoSinTocarLasDemas() {
    Tarifa vieja =
        tarifa(1, "Vieja", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), 8000, false);
    Tarifa media =
        tarifa(2, "Media", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 11, 3), 10000, false);
    Tarifa actual = tarifa(3, "Actual", HOY, null, 12000, false);
    conTarifas(vieja, media, actual);

    tarifaService.eliminar(2);

    assertEquals(2, enBase.size());
    assertEquals(LocalDate.of(2026, 6, 30), vieja.getFechaFin()); // historial intacto
    assertNull(actual.getFechaFin());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void eliminar_unaPromoFutura_laAnteriorSeExtiendeParaNoDejarHueco() {
    conTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 19), 10000, false),
        tarifa(2, "Promo", LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 5), 15000, true),
        tarifa(3, "Base", LocalDate.of(2027, 1, 6), null, 10000, false));

    tarifaService.eliminar(2);

    List<Tarifa> t = ordenadas();
    assertEquals(2, t.size());
    assertEquals(LocalDate.of(2027, 1, 5), t.get(0).getFechaFin());
    assertEquals(LocalDate.of(2027, 1, 6), t.get(1).getFechaInicio());
    verifyNoInteractions(eventPublisher); // hoy el precio no cambia
  }

  @Test
  void eliminar_laContinuacionAbiertaDespuesDeUnaPromo_laPromoQuedaAbiertaYDejaDeSerEspecial() {
    Tarifa promoPasada = tarifa(2, "Promo", HOY.minusDays(10), HOY.minusDays(1), 8000, true);
    Tarifa continuacion = tarifa(3, "Base", HOY, null, 10000, false);
    conTarifas(promoPasada, continuacion);

    tarifaService.eliminar(3);

    assertNull(promoPasada.getFechaFin());
    assertFalse(promoPasada.getEspecial());
    assertEventoPrecio(eventos(1).get(0), 3, 8000.0);
  }

  // ------------------------------------------------- tarea diaria (cambios de precio)

  private Tarifa vigenteDe(LocalDate dia, Tarifa... candidatas) {
    return java.util.Arrays.stream(candidatas)
        .filter(t -> !t.getFechaInicio().isAfter(dia))
        .filter(t -> t.getFechaFin() == null || !t.getFechaFin().isBefore(dia))
        .findFirst()
        .orElse(null);
  }

  private void tareaConTarifas(Tarifa... tarifas) {
    when(tarifaRepository.findByFechaInicio(HOY))
        .thenReturn(
            java.util.Arrays.stream(tarifas).filter(t -> t.getFechaInicio().equals(HOY)).toList());
    lenient()
        .when(tarifaRepository.findVigentes(eq(3), any(LocalDate.class)))
        .thenAnswer(
            inv -> {
              Tarifa v = vigenteDe(inv.getArgument(1), tarifas);
              return v == null ? List.of() : List.of(v);
            });
  }

  @Test
  void tareaDiaria_empiezaUnaPromoHoy_publicaElPrecioDeLaPromo() {
    tareaConTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 3), 10000, false),
        tarifa(2, "Promo", HOY, LocalDate.of(2026, 11, 10), 8000, true),
        tarifa(3, "Base", LocalDate.of(2026, 11, 11), null, 10000, false));

    assertEquals(1, tarifaService.publicarCambiosDePrecioDelDia());

    assertEventoPrecio(eventos(1).get(0), 3, 8000.0);
  }

  @Test
  void tareaDiaria_terminaLaPromoYVuelveElPrecioComun_publicaElPrecioComun() {
    // hoy es 11/11/2026: arranca la continuación de la promo
    ReflectionTestUtils.setField(
        tarifaService,
        "clock",
        Clock.fixed(LocalDate.of(2026, 11, 11).atStartOfDay(ZONA).toInstant(), ZONA));
    Tarifa base =
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 3), 10000, false);
    Tarifa promo = tarifa(2, "Promo", HOY, LocalDate.of(2026, 11, 10), 8000, true);
    Tarifa cont = tarifa(3, "Base", LocalDate.of(2026, 11, 11), null, 10000, false);
    when(tarifaRepository.findByFechaInicio(LocalDate.of(2026, 11, 11))).thenReturn(List.of(cont));
    when(tarifaRepository.findVigentes(eq(3), any(LocalDate.class)))
        .thenAnswer(
            inv -> {
              Tarifa v = vigenteDe(inv.getArgument(1), base, promo, cont);
              return v == null ? List.of() : List.of(v);
            });

    assertEquals(1, tarifaService.publicarCambiosDePrecioDelDia());

    assertEventoPrecio(eventos(1).get(0), 3, 10000.0);
  }

  @Test
  void tareaDiaria_empiezaUnaTarifaConElMismoPrecioQueAyer_noPublicaNada() {
    tareaConTarifas(
        tarifa(1, "Base", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 3), 10000, false),
        tarifa(2, "Base", HOY, null, 10000, false));

    assertEquals(0, tarifaService.publicarCambiosDePrecioDelDia());

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void tareaDiaria_sinTarifasQueEmpiecenHoy_noHaceNada() {
    when(tarifaRepository.findByFechaInicio(HOY)).thenReturn(List.of());

    assertEquals(0, tarifaService.publicarCambiosDePrecioDelDia());

    verifyNoInteractions(eventPublisher);
    verify(tarifaRepository, never()).findVigentes(any(), any());
  }

  @Test
  void tareaDiaria_primeraTarifaDelTipo_publicaElPrecio() {
    tareaConTarifas(tarifa(1, "Base", HOY, null, 10000, false));

    assertEquals(1, tarifaService.publicarCambiosDePrecioDelDia());

    assertEventoPrecio(eventos(1).get(0), 3, 10000.0);
  }

  @Test
  void tareaDiaria_dosTarifasQueEmpiezanHoyDelMismoTipo_publicaUnaSolaVez() {
    Tarifa a = tarifa(1, "Base", HOY, null, 10000, false);
    Tarifa b = tarifa(2, "Otra", HOY, null, 10000, false);
    when(tarifaRepository.findByFechaInicio(HOY)).thenReturn(List.of(a, b));
    when(tarifaRepository.findVigentes(eq(3), any(LocalDate.class)))
        .thenAnswer(inv -> inv.getArgument(1).equals(HOY) ? List.of(a) : List.of());

    assertEquals(1, tarifaService.publicarCambiosDePrecioDelDia());
  }

  // El reloj usado por el servicio es el de la zona de negocio (no el del servidor)
  @Test
  void hoySeCalculaConLaZonaDelReloj() {
    // 02:00 UTC del 5/11 = 23:00 del 4/11 en Argentina (UTC-3)
    ReflectionTestUtils.setField(
        tarifaService, "clock", Clock.fixed(Instant.parse("2026-11-05T02:00:00Z"), ZONA));
    when(tarifaRepository.findVigentes(3, LocalDate.of(2026, 11, 4))).thenReturn(List.of());

    tarifaService.obtenerTarifaVigente(3);

    verify(tarifaRepository).findVigentes(3, LocalDate.of(2026, 11, 4));
  }
}
