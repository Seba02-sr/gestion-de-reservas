package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import edu.utn.frsf.isi.dan.reservas_svc.dto.BusquedaHabitacionesRequest;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.repository.HabitacionRepository;
import edu.utn.frsf.isi.dan.reservas_svc.testutil.MongoQueries;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TarifaDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

  @Mock private HabitacionRepository habitacionRepository;
  @Mock private MongoTemplate mongoTemplate;
  @InjectMocks private HabitacionService habitacionService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        habitacionService,
        "clock",
        Clock.fixed(LocalDate.of(2026, 11, 4).atTime(12, 0).atZone(ZONA).toInstant(), ZONA));
  }

  private static HotelDTO hotelSantaFe() {
    return HotelDTO.builder()
        .id(1)
        .nombre("Hotel Castelar")
        .domicilio("Bv. Pellegrini 2500")
        .latitud(-31.63)
        .longitud(-60.70)
        .categoria(4)
        .build();
  }

  private static HabitacionDTO dto() {
    return HabitacionDTO.builder()
        .habitacionId(10L)
        .numero(101)
        .piso(2)
        .disponible(true)
        .capacidad(2)
        .precioNoche(15000.0)
        .tipoHabitacionId(3)
        .tipoHabitacion("DOBLE")
        .amenities(List.of("WIFI", "PILETA"))
        .hotel(hotelSantaFe())
        .build();
  }

  private static Document set(UpdateDefinition update) {
    return (Document) update.getUpdateObject().get("$set");
  }

  // ------------------------------------------------------------------ mapeo

  @Test
  void mapFromDto_guardaLaUbicacionComoLongitudLatitud() {
    Hotel hotel = habitacionService.mapFromDto(hotelSantaFe());

    GeoJsonPoint punto = hotel.getUbicacion();
    assertEquals(-60.70, punto.getX()); // x = longitud
    assertEquals(-31.63, punto.getY()); // y = latitud
    assertEquals(List.of(-60.70, -31.63), punto.getCoordinates());
  }

  @Test
  void mapFromDto_sinCoordenadas_dejaLaUbicacionNula() {
    HotelDTO sinCoordenadas = HotelDTO.builder().id(1).nombre("X").build();

    assertNull(habitacionService.mapFromDto(sinCoordenadas).getUbicacion());
    assertNull(habitacionService.mapFromDto(null));
  }

  @Test
  void mapFromHabitacion_incluyeNumeroPisoYDisponible() {
    Habitacion h = habitacionService.mapFromHabitacion(dto());

    assertEquals(10L, h.getHabitacionId());
    assertEquals(101, h.getNumero());
    assertEquals(2, h.getPiso());
    assertEquals(true, h.getDisponible());
    assertEquals(3, h.getIdTipoHabitacion());
    assertEquals("DOBLE", h.getTipoHabitacion());
    assertEquals(15000.0, h.getPrecioNoche());
    assertEquals(List.of("WIFI", "PILETA"), h.getAmenities());
  }

  // ------------------------------------------------------------------ CREAR

  @Test
  void crear_haceUpsertPorHabitacionIdYSoloInicializaReservasAlInsertar() {
    when(mongoTemplate.upsert(any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(UpdateResult.acknowledged(0, 0L, null));

    habitacionService.handleEvent(
        HabitacionEvent.builder().tipoEvento(TipoEvento.CREAR).habitacion(dto()).build());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
    verify(mongoTemplate).upsert(query.capture(), update.capture(), eq(Habitacion.class));
    assertEquals(10L, query.getValue().getQueryObject().get("habitacionId"));
    Document set = set(update.getValue());
    assertEquals(101, set.get("numero"));
    assertEquals(2, set.get("piso"));
    assertEquals(true, set.get("disponible"));
    assertEquals(15000.0, set.get("precioNoche"));
    assertTrue(set.containsKey("hotel"));
    // las reservas solo se inicializan en el insert: un CREAR repetido no las borra
    Document alInsertar = (Document) update.getValue().getUpdateObject().get("$setOnInsert");
    assertEquals(List.of(), alInsertar.get("reservas"));
    assertFalse(set.containsKey("reservas"));
  }

  @Test
  void crear_elMismoMensajeDosVeces_hacePorAmbasVecesElMismoUpsert_sinInsertSuelto() {
    habitacionService.handleEvent(
        HabitacionEvent.builder().tipoEvento(TipoEvento.CREAR).habitacion(dto()).build());
    habitacionService.handleEvent(
        HabitacionEvent.builder().tipoEvento(TipoEvento.CREAR).habitacion(dto()).build());

    verify(mongoTemplate, times(2))
        .upsert(any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class));
    // nunca inserta directamente (eso duplicaría); solo upserts por habitacionId
    verify(mongoTemplate, never()).insert(any(Habitacion.class));
    verify(habitacionRepository, never()).save(any());
  }

  // ------------------------------------------------------- ACTUALIZAR_DATOS

  @Test
  void actualizarDatos_refrescaNumeroPisoDisponibleYHotelConLaUbicacionCorrecta() {
    when(mongoTemplate.findAndModify(
            any(Query.class),
            any(UpdateDefinition.class),
            any(FindAndModifyOptions.class),
            eq(Habitacion.class)))
        .thenReturn(new Habitacion());

    habitacionService.handleEvent(
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_DATOS)
            .habitacion(dto())
            .build());

    ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
    verify(mongoTemplate)
        .findAndModify(
            any(Query.class),
            update.capture(),
            any(FindAndModifyOptions.class),
            eq(Habitacion.class));
    Document set = set(update.getValue());
    assertEquals(101, set.get("numero"));
    assertEquals(2, set.get("piso"));
    assertEquals(true, set.get("disponible"));
    assertEquals(3, set.get("idTipoHabitacion"));
    assertEquals("DOBLE", set.get("tipoHabitacion"));
    Hotel hotel = (Hotel) set.get("hotel");
    assertEquals(-60.70, hotel.getUbicacion().getX());
  }

  @Test
  void actualizarDatos_siLaHabitacionNoExiste_lanzaIllegalArgument() {
    HabitacionEvent evento =
        HabitacionEvent.builder().tipoEvento(TipoEvento.ACTUALIZAR_DATOS).habitacion(dto()).build();

    assertThrows(IllegalArgumentException.class, () -> habitacionService.handleEvent(evento));
  }

  // ------------------------------------------------------- ACTUALIZAR_PRECIO

  @Test
  void handleEvent_actualizarPrecio_actualizaTodasLasHabitacionesDelTipo() {
    when(mongoTemplate.updateMulti(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(UpdateResult.acknowledged(3, 3L, null));
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).nuevoPrecio(18000.0).build())
            .build();

    habitacionService.handleEvent(evento);

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
    verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Habitacion.class));
    assertEquals(3, query.getValue().getQueryObject().get("idTipoHabitacion"));
    assertEquals(18000.0, set(update.getValue()).get("precioNoche"));
  }

  @Test
  void handleEvent_actualizarPrecio_sinDatosLanzaIllegalArgument() {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).build())
            .build();

    assertThrows(IllegalArgumentException.class, () -> habitacionService.handleEvent(evento));
  }

  // ------------------------------------------------------------ CERRAR_HOTEL

  private static HabitacionEvent cerrar(Long... ids) {
    return HabitacionEvent.builder()
        .tipoEvento(TipoEvento.CERRAR_HOTEL)
        .hotelId(1)
        .habitacionIds(List.of(ids))
        .fechaCierre(LocalDate.of(2026, 11, 4))
        .build();
  }

  @Test
  void cerrarHotel_porCadaHabitacionAgregaUnaReservaCerradaYInsertaLaReserva() {
    when(mongoTemplate.findAndModify(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(new Habitacion());

    habitacionService.handleEvent(cerrar(10L, 11L));

    ArgumentCaptor<Reserva> reservas = ArgumentCaptor.forClass(Reserva.class);
    verify(mongoTemplate, times(2)).insert(reservas.capture());
    Instant esperado = LocalDate.of(2026, 11, 4).atStartOfDay(ZONA).toInstant();
    assertEquals(
        List.of(10L, 11L), reservas.getAllValues().stream().map(Reserva::getHabitacionId).toList());
    for (Reserva r : reservas.getAllValues()) {
      assertEquals(EstadoReserva.CERRADA, r.getEstadoReserva());
      assertEquals(esperado, r.getCheckIn());
      assertNull(r.getCheckOut());
      assertNull(r.getHuesped());
      assertNull(r.getPrecioTotal());
      assertEquals(1, r.getHotelId());
      assertNotNull(r.get_id());
    }
  }

  @Test
  void cerrarHotel_laCopiaEmbebidaYLaReservaComparteElId_yLaHabitacionQuedaNoDisponible() {
    when(mongoTemplate.findAndModify(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(new Habitacion());

    habitacionService.handleEvent(cerrar(10L));

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
    ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
    verify(mongoTemplate).findAndModify(query.capture(), update.capture(), eq(Habitacion.class));
    verify(mongoTemplate).insert(reserva.capture());
    assertEquals(10L, query.getValue().getQueryObject().get("habitacionId"));
    assertTrue(
        query.getValue().getQueryObject().toJson().contains("$nor")); // solo si no estaba cerrada
    assertEquals(false, set(update.getValue()).get("disponible"));
    Habitacion.ReservaSimple simple =
        (Habitacion.ReservaSimple)
            ((Document) update.getValue().getUpdateObject().get("$push")).get("reservas");
    assertEquals(reserva.getValue().get_id(), simple.getReservaId());
    assertEquals(EstadoReserva.CERRADA, simple.getEstadoReserva());
    assertNull(simple.getCheckOut());
  }

  @Test
  void cerrarHotel_llegandoDosVeces_noDuplicaLaReserva() {
    when(mongoTemplate.findAndModify(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(new Habitacion()) // primera vez: la habitación se modificó
        .thenReturn(null); // segunda vez: ya tenía una CERRADA, la condición no matchea

    habitacionService.handleEvent(cerrar(10L));
    habitacionService.handleEvent(cerrar(10L));

    verify(mongoTemplate, times(2))
        .findAndModify(any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class));
    verify(mongoTemplate, times(1)).insert(any(Reserva.class));
  }

  @Test
  void cerrarHotel_conUnaHabitacionInexistente_siguePorLasDemas() {
    when(mongoTemplate.findAndModify(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(null) // la 10 no está en la réplica
        .thenReturn(new Habitacion());

    assertDoesNotThrow(() -> habitacionService.handleEvent(cerrar(10L, 11L)));

    ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
    verify(mongoTemplate, times(1)).insert(reserva.capture());
    assertEquals(11L, reserva.getValue().getHabitacionId());
  }

  @Test
  void cerrarHotel_sinFechaDeCierre_usaHoy() {
    when(mongoTemplate.findAndModify(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(new Habitacion());
    HabitacionEvent sinFecha =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CERRAR_HOTEL)
            .hotelId(1)
            .habitacionIds(List.of(10L))
            .build();

    habitacionService.handleEvent(sinFecha);

    ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
    verify(mongoTemplate).insert(reserva.capture());
    assertEquals(
        LocalDate.of(2026, 11, 4).atStartOfDay(ZONA).toInstant(), reserva.getValue().getCheckIn());
  }

  @Test
  void cerrarHotel_sinHabitaciones_noHaceNada() {
    habitacionService.handleEvent(
        HabitacionEvent.builder().tipoEvento(TipoEvento.CERRAR_HOTEL).hotelId(1).build());

    verifyNoInteractions(mongoTemplate);
  }

  // ----------------------------------------------------------------- búsqueda

  private Document consulta(BusquedaHabitacionesRequest f) {
    return MongoQueries.mapear(habitacionService.construirQuery(f));
  }

  @SuppressWarnings("unchecked")
  private static List<Document> condiciones(Document consulta) {
    return (List<Document>) consulta.get("$and");
  }

  private static Document condicionSobre(Document consulta, String campo) {
    return condiciones(consulta).stream()
        .filter(d -> d.containsKey(campo))
        .findFirst()
        .orElseThrow(() -> new AssertionError("sin condición sobre " + campo + ": " + consulta));
  }

  @Test
  void busqueda_sinFiltros_soloExigeQueNoEsteMarcadaComoNoDisponible() {
    List<Document> cond = condiciones(consulta(BusquedaHabitacionesRequest.builder().build()));

    assertEquals(1, cond.size());
    assertEquals(new Document("disponible", new Document("$ne", false)), cond.get(0));
  }

  @Test
  void busqueda_porCapacidadPrecioEstrellasYAmenities() {
    Document q =
        consulta(
            BusquedaHabitacionesRequest.builder()
                .cantidadHuespedes(3)
                .precioMin(10000.0)
                .precioMax(20000.0)
                .estrellas(4)
                .amenities(List.of("wifi", " pileta "))
                .build());

    assertEquals(new Document("$gte", 3), condicionSobre(q, "capacidad").get("capacidad"));
    assertEquals(
        new Document("$gte", 10000.0).append("$lte", 20000.0),
        condicionSobre(q, "precioNoche").get("precioNoche"));
    assertEquals(
        new Document("$gte", 4), condicionSobre(q, "hotel.categoria").get("hotel.categoria"));
    assertEquals(
        new Document("$all", List.of("WIFI", "PILETA")),
        condicionSobre(q, "amenities").get("amenities"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void busqueda_porCercania_usaGeoWithinConCenterSphereConElOrdenLongitudLatitud() {
    Document q =
        consulta(
            BusquedaHabitacionesRequest.builder().lat(-31.63).lng(-60.70).radioKm(5.0).build());

    Document geo = (Document) condicionSobre(q, "hotel.ubicacion").get("hotel.ubicacion");
    Document within = (Document) geo.get("$geoWithin");
    List<Object> centerSphere = (List<Object>) within.get("$centerSphere");
    List<Double> centro = (List<Double>) centerSphere.get(0);
    assertEquals(-60.70, centro.get(0)); // x = longitud
    assertEquals(-31.63, centro.get(1)); // y = latitud
    assertEquals(5.0 / 6378.137, (Double) centerSphere.get(1), 1e-9); // km → radianes
  }

  @Test
  void busqueda_porFechas_excluyeLasHabitacionesConReservasQueSuperponen() {
    Document q =
        consulta(
            BusquedaHabitacionesRequest.builder()
                .checkIn(LocalDate.of(2026, 12, 10))
                .checkOut(LocalDate.of(2026, 12, 15))
                .build());

    Document nor = condicionSobre(q, "$nor");
    String json = nor.toString();
    assertTrue(json.contains("$elemMatch"), json);
    assertTrue(json.contains("CANCELADA"), json);
    assertTrue(json.contains("checkOut"), json);
  }

  @Test
  void busqueda_combinaTodosLosFiltrosConAnd() {
    Document q =
        consulta(
            BusquedaHabitacionesRequest.builder()
                .checkIn(LocalDate.of(2026, 12, 10))
                .checkOut(LocalDate.of(2026, 12, 15))
                .cantidadHuespedes(2)
                .precioMax(20000.0)
                .estrellas(3)
                .amenities(List.of("WIFI"))
                .lat(-31.63)
                .lng(-60.70)
                .radioKm(10.0)
                .build());

    // disponible + fechas + capacidad + precio + estrellas + amenities + geo
    assertEquals(7, condiciones(q).size());
  }

  @Test
  void busqueda_validaCombinacionesInvalidas() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            consulta(
                BusquedaHabitacionesRequest.builder().checkIn(LocalDate.of(2026, 12, 10)).build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            consulta(
                BusquedaHabitacionesRequest.builder()
                    .checkIn(LocalDate.of(2026, 12, 15))
                    .checkOut(LocalDate.of(2026, 12, 10))
                    .build()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            consulta(BusquedaHabitacionesRequest.builder().precioMin(5.0).precioMax(1.0).build()));
    assertThrows(
        IllegalArgumentException.class,
        () -> consulta(BusquedaHabitacionesRequest.builder().lat(-31.6).lng(-60.7).build()));
    assertThrows(
        IllegalArgumentException.class,
        () -> consulta(BusquedaHabitacionesRequest.builder().radioKm(3.0).build()));
  }
}
