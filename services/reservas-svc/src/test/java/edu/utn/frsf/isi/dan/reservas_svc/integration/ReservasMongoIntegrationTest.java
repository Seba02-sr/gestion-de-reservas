package edu.utn.frsf.isi.dan.reservas_svc.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.utn.frsf.isi.dan.reservas_svc.client.GestionServiceClient;
import edu.utn.frsf.isi.dan.reservas_svc.client.HuespedInfo;
import edu.utn.frsf.isi.dan.reservas_svc.client.UserServiceClient;
import edu.utn.frsf.isi.dan.reservas_svc.dto.BloqueoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.BusquedaHabitacionesRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.PagoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReviewRequest;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion.ReservaSimple;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.service.DisponibilidadTest;
import edu.utn.frsf.isi.dan.reservas_svc.service.HabitacionService;
import edu.utn.frsf.isi.dan.reservas_svc.service.ReservaService;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TarifaDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Pruebas contra un MongoDB real (contenedor {@code mongo:7}, standalone como el del compose). Se
 * saltean si no hay Docker disponible. Validan lo que un mock no puede: la semántica real de {@code
 * $nor/$elemMatch/$or}, {@code $geoWithin}, {@code $pull}, el update posicional, los índices y la
 * atomicidad de la reserva concurrente.
 */
@SpringBootTest(
    properties = {
      "spring.rabbitmq.listener.simple.auto-startup=false",
      "reservas.crear-indices=true"
    })
class ReservasMongoIntegrationTest {

  private static final ZoneId BA = ZoneId.of("America/Argentina/Buenos_Aires");

  private static GenericContainer<?> mongo;

  @BeforeAll
  static void arrancarMongo() {
    assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker no está disponible: se saltean los tests de integración con MongoDB");
    mongo =
        new GenericContainer<>("mongo:7")
            .withExposedPorts(27017)
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
    mongo.start();
  }

  @AfterAll
  static void detenerMongo() {
    if (mongo != null) {
      mongo.stop();
    }
  }

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.data.mongodb.uri",
        () -> "mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017) + "/reservas_it");
  }

  @Autowired private MongoTemplate mongoTemplate;
  @Autowired private HabitacionService habitacionService;
  @Autowired private ReservaService reservaService;

  // user-svc y gestion-svc no participan en estas pruebas: se simulan sus respuestas
  @MockitoBean private UserServiceClient userClient;
  @MockitoBean private GestionServiceClient gestionClient;

  private final AtomicInteger secuencia = new AtomicInteger(1000);

  @BeforeEach
  void limpiar() {
    org.mockito.Mockito.when(userClient.getHuesped(org.mockito.ArgumentMatchers.anyInt()))
        .thenAnswer(inv -> new HuespedInfo(inv.getArgument(0), "Huésped Test", "test@example.com"));
    org.mockito.Mockito.when(
            gestionClient.habitacionDisponible(org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(true);
    // se vacían las colecciones sin borrarlas, para conservar los índices creados al arrancar
    mongoTemplate.remove(new Query(), Habitacion.class);
    mongoTemplate.remove(new Query(), Reserva.class);
  }

  // ------------------------------------------------------------------ helpers

  private static LocalDate hoy() {
    return LocalDate.now(BA);
  }

  private static HotelDTO santaFe() {
    return HotelDTO.builder()
        .id(1)
        .nombre("Hotel Castelar")
        .domicilio("Bv. Pellegrini 2500")
        .latitud(-31.6333)
        .longitud(-60.7)
        .categoria(4)
        .build();
  }

  private static HotelDTO buenosAires() {
    return HotelDTO.builder()
        .id(2)
        .nombre("Hotel Porteño")
        .domicilio("Av. Corrientes 1234")
        .latitud(-34.6037)
        .longitud(-58.3816)
        .categoria(5)
        .build();
  }

  private HabitacionDTO dto(
      Long id, HotelDTO hotel, Double precio, int capacidad, String... amenities) {
    return HabitacionDTO.builder()
        .habitacionId(id)
        .numero(secuencia.incrementAndGet())
        .piso(1)
        .disponible(true)
        .capacidad(capacidad)
        .precioNoche(precio)
        .tipoHabitacionId(3)
        .tipoHabitacion("DOBLE")
        .amenities(List.of(amenities))
        .hotel(hotel)
        .build();
  }

  private void crear(HabitacionDTO dto) {
    habitacionService.handleEvent(
        HabitacionEvent.builder().tipoEvento(TipoEvento.CREAR).habitacion(dto).build());
  }

  private Habitacion habitacion(Long id) {
    return habitacionService.findByHabitacionId(id).orElseThrow();
  }

  private List<Long> buscar(BusquedaHabitacionesRequest filtros) {
    return habitacionService.buscarDisponibles(filtros).stream()
        .map(Habitacion::getHabitacionId)
        .sorted()
        .toList();
  }

  private static BusquedaHabitacionesRequest fechas(LocalDate in, LocalDate out) {
    return BusquedaHabitacionesRequest.builder().checkIn(in).checkOut(out).build();
  }

  private ReservaResponse reservar(Long habitacionId, LocalDate in, LocalDate out) {
    return reservaService.crear(
        ReservaRequest.builder()
            .habitacionId(habitacionId)
            .idHuesped(7)
            .checkIn(in)
            .checkOut(out)
            .build());
  }

  private static PagoRequest pago(double monto) {
    return PagoRequest.builder().method("TARJETA").monto(monto).build();
  }

  private static ReviewRequest review() {
    return ReviewRequest.builder().rating(5.0).comentario("Excelente").build();
  }

  // ------------------------------------------------------------------ índices

  @Test
  void alArrancarSeCreanLosIndices_2dsphereYHabitacionIdUnico() {
    List<IndexInfo> habitacion = mongoTemplate.indexOps("habitacion").getIndexInfo();
    List<String> nombres = habitacion.stream().map(IndexInfo::getName).toList();

    assertTrue(nombres.contains("hotel_ubicacion_2dsphere"), nombres.toString());
    assertTrue(nombres.contains("habitacionId_unico"), nombres.toString());
    IndexInfo geo =
        habitacion.stream()
            .filter(i -> i.getName().equals("hotel_ubicacion_2dsphere"))
            .findFirst()
            .get();
    assertTrue(geo.getIndexFields().get(0).isGeo());
    assertTrue(
        habitacion.stream()
            .filter(i -> i.getName().equals("habitacionId_unico"))
            .findFirst()
            .get()
            .isUnique());
    assertTrue(
        mongoTemplate.indexOps("reserva").getIndexInfo().stream()
            .anyMatch(i -> i.getName().equals("reserva_habitacionId")));
  }

  // ------------------------------------------------------------------- CREAR

  @Test
  void crear_dosVeces_noDuplicaLaHabitacion_ni_borraLasReservas() {
    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI"));
    reservar(1L, hoy().plusDays(10), hoy().plusDays(12));

    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI")); // mensaje repetido

    assertEquals(
        1, mongoTemplate.count(new Query(Criteria.where("habitacionId").is(1L)), Habitacion.class));
    assertEquals(
        1, habitacion(1L).getReservas().size(), "el CREAR repetido no debe borrar reservas");
  }

  @Test
  void crear_guardaLaUbicacionComoLongitudLatitudEnMongo() {
    crear(dto(1L, santaFe(), 15000.0, 2));

    org.bson.Document doc =
        mongoTemplate
            .getCollection("habitacion")
            .find(new org.bson.Document("habitacionId", 1L))
            .first();
    org.bson.Document ubicacion =
        (org.bson.Document) ((org.bson.Document) doc.get("hotel")).get("ubicacion");
    assertEquals("Point", ubicacion.get("type"));
    assertEquals(List.of(-60.7, -31.6333), ubicacion.get("coordinates")); // [lng, lat]
    assertEquals(101 > 0, doc.get("numero") != null);
    assertEquals(true, doc.get("disponible"));
  }

  @Test
  void actualizarDatos_conservaLasReservasYRefrescaElHotel() {
    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI"));
    reservar(1L, hoy().plusDays(10), hoy().plusDays(12));

    HotelDTO renombrado = santaFe();
    renombrado.setCategoria(5);
    habitacionService.handleEvent(
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_DATOS)
            .habitacion(dto(1L, renombrado, 15000.0, 2, "WIFI", "PILETA"))
            .build());

    Habitacion h = habitacion(1L);
    assertEquals(5, h.getHotel().getCategoria());
    assertEquals(List.of("WIFI", "PILETA"), h.getAmenities());
    assertEquals(1, h.getReservas().size());
  }

  @Test
  void actualizarPrecio_cambiaElPrecioDeTodasLasHabitacionesDelTipo() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    crear(dto(2L, santaFe(), 15000.0, 2));

    habitacionService.handleEvent(
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).nuevoPrecio(18000.0).build())
            .build());

    assertEquals(18000.0, habitacion(1L).getPrecioNoche());
    assertEquals(18000.0, habitacion(2L).getPrecioNoche());
  }

  // --------------------------------------------------------------------- geo

  @Test
  void busquedaPorCercania_encuentraSantaFeYNoBuenosAires_yElOrdenLngLatImporta() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    crear(dto(2L, buenosAires(), 25000.0, 2));

    assertEquals(
        List.of(1L),
        buscar(BusquedaHabitacionesRequest.builder().lat(-31.63).lng(-60.70).radioKm(5.0).build()));
    assertEquals(
        List.of(2L),
        buscar(BusquedaHabitacionesRequest.builder().lat(-34.60).lng(-58.38).radioKm(5.0).build()));
    // Santa Fe - Buenos Aires: ~400 km
    assertEquals(
        List.of(1L, 2L),
        buscar(BusquedaHabitacionesRequest.builder().lat(-32.5).lng(-59.5).radioKm(500.0).build()));
    assertEquals(
        List.of(1L),
        buscar(
            BusquedaHabitacionesRequest.builder().lat(-31.63).lng(-60.70).radioKm(300.0).build()));
    // lat y lng invertidas: apunta al medio del océano, no encuentra nada
    assertEquals(
        List.of(),
        buscar(
            BusquedaHabitacionesRequest.builder().lat(-60.70).lng(-31.63).radioKm(50.0).build()));
  }

  @Test
  void busquedaPorCercania_secombinaConLosDemasFiltros() {
    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI"));
    crear(dto(2L, santaFe(), 30000.0, 4, "WIFI", "PILETA"));
    crear(dto(3L, buenosAires(), 15000.0, 2, "WIFI"));

    BusquedaHabitacionesRequest.BusquedaHabitacionesRequestBuilder cerca =
        BusquedaHabitacionesRequest.builder().lat(-31.63).lng(-60.70).radioKm(10.0);

    assertEquals(List.of(1L, 2L), buscar(cerca.build()));
    assertEquals(List.of(2L), buscar(cerca.cantidadHuespedes(3).build()));
    assertEquals(
        List.of(2L), buscar(cerca.cantidadHuespedes(null).amenities(List.of("pileta")).build()));
    assertEquals(
        List.of(1L), buscar(cerca.amenities(null).precioMin(10000.0).precioMax(20000.0).build()));
    assertEquals(
        List.of(1L, 2L), buscar(cerca.precioMin(null).precioMax(null).estrellas(4).build()));
    assertEquals(List.of(), buscar(cerca.estrellas(5).build()));
  }

  @Test
  void busquedaPorHotel_devuelveSoloLasHabitacionesDeEseHotel_yCombinaConOtrosFiltros() {
    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI"));
    crear(dto(2L, santaFe(), 30000.0, 4, "WIFI", "PILETA"));
    crear(dto(3L, buenosAires(), 15000.0, 2, "WIFI"));

    assertEquals(List.of(1L, 2L), buscar(BusquedaHabitacionesRequest.builder().hotelId(1).build()));
    assertEquals(List.of(3L), buscar(BusquedaHabitacionesRequest.builder().hotelId(2).build()));
    assertEquals(List.of(), buscar(BusquedaHabitacionesRequest.builder().hotelId(99).build()));
    assertEquals(
        List.of(2L),
        buscar(BusquedaHabitacionesRequest.builder().hotelId(1).cantidadHuespedes(3).build()));
  }

  @Test
  void busquedaPorFiltros_sinGeo() {
    crear(dto(1L, santaFe(), 15000.0, 2, "WIFI"));
    crear(dto(2L, buenosAires(), 30000.0, 4, "WIFI", "PILETA", "SPA"));
    crear(dto(3L, santaFe(), null, 2, "WIFI")); // sin tarifa

    assertEquals(List.of(1L, 2L, 3L), buscar(BusquedaHabitacionesRequest.builder().build()));
    assertEquals(
        List.of(2L), buscar(BusquedaHabitacionesRequest.builder().cantidadHuespedes(3).build()));
    assertEquals(
        List.of(2L),
        buscar(BusquedaHabitacionesRequest.builder().amenities(List.of("WIFI", "SPA")).build()));
    assertEquals(
        List.of(1L, 2L),
        buscar(BusquedaHabitacionesRequest.builder().precioMin(1.0).precioMax(40000.0).build()),
        "con filtro de precio se excluyen las que no tienen tarifa");
    assertEquals(List.of(2L), buscar(BusquedaHabitacionesRequest.builder().estrellas(5).build()));
  }

  // ------------------------------------------------- superposición contra Mongo

  @Test
  void laTablaDeSuperposicionDaLoMismoEnJavaQueEnMongo() {
    long id = 5000;
    for (DisponibilidadTest.Caso c : DisponibilidadTest.CASOS) {
      id++;
      Habitacion h =
          Habitacion.builder()
              .habitacionId(id)
              .numero((int) id)
              .disponible(true)
              .precioNoche(1000.0)
              .reservas(
                  List.of(
                      ReservaSimple.builder()
                          .reservaId("r" + id)
                          .checkIn(DisponibilidadTest.dia(c.resIn()))
                          .checkOut(DisponibilidadTest.dia(c.resOut()))
                          .estadoReserva(c.estado())
                          .build()))
              .build();
      mongoTemplate.insert(h);

      List<Long> libres = buscar(fechas(LocalDate.parse(c.pedIn()), LocalDate.parse(c.pedOut())));

      assertEquals(!c.ocupada(), libres.contains(id), c.nombre());
    }
  }

  // -------------------------------------------------------- concurrencia

  @Test
  void diezReservasSimultaneasDeLasMismasFechas_exactamenteUnaGana() throws Exception {
    crear(dto(1L, santaFe(), 15000.0, 2));
    LocalDate in = hoy().plusDays(30);
    LocalDate out = hoy().plusDays(35);
    int hilos = 10;

    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    CountDownLatch largada = new CountDownLatch(1);
    List<Future<Boolean>> resultados = new ArrayList<>();
    for (int i = 0; i < hilos; i++) {
      resultados.add(
          pool.submit(
              () -> {
                largada.await();
                try {
                  reservar(1L, in, out);
                  return true;
                } catch (ConflictoException e) {
                  return false;
                }
              }));
    }
    largada.countDown();
    long exitos = 0;
    for (Future<Boolean> f : resultados) {
      if (f.get(30, TimeUnit.SECONDS)) {
        exitos++;
      }
    }
    pool.shutdown();

    assertEquals(1, exitos, "solo una reserva puede quedarse con las fechas");
    assertEquals(1, habitacion(1L).getReservas().size());
    assertEquals(1, mongoTemplate.count(new Query(), Reserva.class));
  }

  @Test
  void reservasDeFechasQueNoSeSuperponen_convivenYElCambioDeHuespedElMismoDiaEsValido() {
    crear(dto(1L, santaFe(), 15000.0, 2));

    reservar(1L, hoy().plusDays(10), hoy().plusDays(15));
    assertDoesNotThrow(
        () -> reservar(1L, hoy().plusDays(15), hoy().plusDays(18))); // sale el 15, entra el 15
    assertDoesNotThrow(() -> reservar(1L, hoy().plusDays(5), hoy().plusDays(10)));
    assertThrows(
        ConflictoException.class, () -> reservar(1L, hoy().plusDays(14), hoy().plusDays(16)));
    assertThrows(
        ConflictoException.class, () -> reservar(1L, hoy().plusDays(11), hoy().plusDays(12)));
    assertEquals(3, habitacion(1L).getReservas().size());
  }

  // ------------------------------------------------------------ flujo completo

  @Test
  void flujoCompleto_reservarPagarFinalizar_sincronizandoLaCopiaEmbebida() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    LocalDate in = hoy().plusDays(10);
    LocalDate out = hoy().plusDays(15);

    ReservaResponse r = reservar(1L, in, out);
    assertEquals(EstadoReserva.RESERVADA, r.estado());
    assertEquals(75000.0, r.precioTotal());
    assertEquals(EstadoReserva.RESERVADA, habitacion(1L).getReservas().get(0).getEstadoReserva());
    assertFalse(buscar(fechas(in, out)).contains(1L), "ya no aparece libre en esas fechas");
    assertTrue(
        buscar(fechas(out, out.plusDays(2))).contains(1L), "el día de la salida sí está libre");

    assertEquals(EstadoReserva.CONFIRMADA, reservaService.agregarPago(r.id(), pago(1000)).estado());
    assertEquals(EstadoReserva.CONFIRMADA, habitacion(1L).getReservas().get(0).getEstadoReserva());

    // con pagos no se puede cancelar
    assertThrows(
        ConflictoException.class,
        () -> reservaService.cambiarEstado(r.id(), EstadoReserva.CANCELADA));
    // el pago no puede exceder el saldo
    assertThrows(
        ConflictoException.class, () -> reservaService.agregarPago(r.id(), pago(74000.01)));

    // finalizar sin review del dueño y con pago incompleto -> ADEUDADA
    assertEquals(
        EstadoReserva.ADEUDADA,
        reservaService.cambiarEstado(r.id(), EstadoReserva.FINALIZADA).estado());
    assertEquals(EstadoReserva.ADEUDADA, habitacion(1L).getReservas().get(0).getEstadoReserva());

    // se completa el pago y el review del dueño, y se reintenta
    reservaService.agregarPago(r.id(), pago(74000));
    reservaService.reviewDueno(r.id(), review());
    ReservaResponse fin = reservaService.cambiarEstado(r.id(), EstadoReserva.FINALIZADA);
    assertEquals(EstadoReserva.FINALIZADA, fin.estado());
    assertEquals(75000.0, fin.totalPagado());
    assertEquals(0.0, fin.saldo());
    assertEquals(EstadoReserva.FINALIZADA, habitacion(1L).getReservas().get(0).getEstadoReserva());

    // la calificación del huésped recién después del check-out
    assertThrows(ConflictoException.class, () -> reservaService.reviewCliente(r.id(), review()));
    assertEquals(fin.id(), reservaService.obtener(r.id()).id());
    assertEquals(1, reservaService.listarPorHuesped(7).size());
  }

  @Test
  void cancelar_liberaLasFechas_yLaCancelacionNoOcupa() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    LocalDate in = hoy().plusDays(10);
    LocalDate out = hoy().plusDays(15);
    ReservaResponse r = reservar(1L, in, out);

    assertEquals(
        EstadoReserva.CANCELADA,
        reservaService.cambiarEstado(r.id(), EstadoReserva.CANCELADA).estado());

    assertEquals(0, habitacion(1L).getReservas().size());
    assertTrue(buscar(fechas(in, out)).contains(1L));
    assertDoesNotThrow(() -> reservar(1L, in, out)); // se puede volver a reservar
  }

  @Test
  void finalizarDesdeReservadaSinPagos_quedaAdeudada() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    ReservaResponse r = reservar(1L, hoy().plusDays(10), hoy().plusDays(12));

    assertEquals(
        EstadoReserva.ADEUDADA,
        reservaService.cambiarEstado(r.id(), EstadoReserva.FINALIZADA).estado());
    assertEquals(EstadoReserva.ADEUDADA, habitacion(1L).getReservas().get(0).getEstadoReserva());
  }

  @Test
  void bloqueo_ocupaLasFechasHastaQueSeCancela() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    LocalDate in = hoy().plusDays(20);
    LocalDate out = hoy().plusDays(22);

    ReservaResponse bloqueo =
        reservaService.crearBloqueo(
            BloqueoRequest.builder().habitacionId(1L).checkIn(in).checkOut(out).build());

    assertEquals(EstadoReserva.BLOQUEADA, bloqueo.estado());
    assertFalse(buscar(fechas(in, out)).contains(1L));
    assertThrows(ConflictoException.class, () -> reservar(1L, in, out));

    reservaService.cambiarEstado(bloqueo.id(), EstadoReserva.CANCELADA);
    assertTrue(buscar(fechas(in, out)).contains(1L));
  }

  // -------------------------------------------------------------- CERRAR_HOTEL

  @Test
  void cerrarHotel_creaUnaReservaCerradaPorHabitacion_esIdempotenteYBloqueaPorSiempre() {
    crear(dto(1L, santaFe(), 15000.0, 2));
    crear(dto(2L, santaFe(), 15000.0, 2));
    crear(dto(3L, buenosAires(), 15000.0, 2)); // otro hotel: no se cierra
    HabitacionEvent cierre =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CERRAR_HOTEL)
            .hotelId(1)
            .habitacionIds(List.of(1L, 2L))
            .fechaCierre(hoy())
            .build();

    habitacionService.handleEvent(cierre);
    habitacionService.handleEvent(cierre); // el mensaje llega dos veces

    for (long id : new long[] {1L, 2L}) {
      Habitacion h = habitacion(id);
      assertEquals(1, h.getReservas().size(), "una sola reserva CERRADA por habitación");
      assertEquals(EstadoReserva.CERRADA, h.getReservas().get(0).getEstadoReserva());
      assertNull(h.getReservas().get(0).getCheckOut());
      assertEquals(false, h.getDisponible());
      List<Reserva> docs =
          mongoTemplate.find(new Query(Criteria.where("habitacionId").is(id)), Reserva.class);
      assertEquals(1, docs.size());
      assertEquals(EstadoReserva.CERRADA, docs.get(0).getEstadoReserva());
      assertEquals(hoy().atStartOfDay(BA).toInstant(), docs.get(0).getCheckIn());
      assertNull(docs.get(0).getCheckOut());
      assertNull(docs.get(0).getHuesped());
      assertEquals(docs.get(0).get_id(), h.getReservas().get(0).getReservaId());
    }
    assertEquals(
        2,
        mongoTemplate.count(
            new Query(Criteria.where("estadoReserva").is("CERRADA")), Reserva.class));

    // ya no aparecen, ni con ni sin fechas; la del otro hotel sigue disponible
    assertEquals(List.of(3L), buscar(BusquedaHabitacionesRequest.builder().build()));
    assertEquals(List.of(3L), buscar(fechas(hoy().plusDays(100), hoy().plusDays(102))));
    assertThrows(
        ConflictoException.class, () -> reservar(1L, hoy().plusDays(10), hoy().plusDays(12)));
    assertDoesNotThrow(() -> reservar(3L, hoy().plusDays(10), hoy().plusDays(12)));
  }

  @Test
  void cerrarHotel_conUnaHabitacionQueNoEstaEnLaReplica_procesaLasDemas() {
    crear(dto(2L, santaFe(), 15000.0, 2));

    assertDoesNotThrow(
        () ->
            habitacionService.handleEvent(
                HabitacionEvent.builder()
                    .tipoEvento(TipoEvento.CERRAR_HOTEL)
                    .hotelId(1)
                    .habitacionIds(List.of(99L, 2L))
                    .fechaCierre(hoy())
                    .build()));

    assertEquals(EstadoReserva.CERRADA, habitacion(2L).getReservas().get(0).getEstadoReserva());
    assertTrue(habitacionService.findByHabitacionId(99L).isEmpty());
  }
}
