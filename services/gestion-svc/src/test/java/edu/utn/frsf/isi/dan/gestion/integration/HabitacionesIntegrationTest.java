package edu.utn.frsf.isi.dan.gestion.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.dto.HotelUpdateRequest;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.gestion.service.HabitacionService;
import edu.utn.frsf.isi.dan.gestion.service.HotelService;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pruebas con la aplicación completa sobre H2 (esquema generado por Hibernate) y RabbitMQ mockeado:
 * verifican que los mensajes salen recién después del commit (y no en un rollback), que la tarifa
 * vigente y los filtros de habitaciones funcionan contra la base y que el número de habitación es
 * único por hotel.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:gestion_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS tp_dan",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "rabbitmq.exchange=dan.exchange",
      "rabbitmq.routingkey.prefix=dan.habitacion"
    })
@ActiveProfiles("test")
class HabitacionesIntegrationTest {

  @MockitoBean private RabbitTemplate rabbitTemplate;
  @MockitoBean private ConnectionFactory connectionFactory;
  @MockitoBean private AmqpAdmin amqpAdmin;

  @Autowired private HabitacionService habitacionService;
  @Autowired private HotelService hotelService;
  @Autowired private TarifaService tarifaService;
  @Autowired private HabitacionRepository habitacionRepository;
  @Autowired private HotelRepository hotelRepository;
  @Autowired private TarifaRepository tarifaRepository;
  @Autowired private TipoHabitacionRepository tipoHabitacionRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ObjectMapper objectMapper;

  private Hotel hotel;
  private TipoHabitacion doble;
  private TipoHabitacion single;

  @BeforeEach
  void setUp() {
    habitacionRepository.deleteAll();
    tarifaRepository.deleteAll();
    hotelRepository.deleteAll();
    tipoHabitacionRepository.deleteAll();

    doble = tipoHabitacionRepository.save(new TipoHabitacion(null, "DOBLE", "Doble una cama", 2));
    single = tipoHabitacionRepository.save(new TipoHabitacion(null, "SINGLE", "Single", 1));

    Hotel nuevo =
        Hotel.builder()
            .nombre("Hotel Santa Fe")
            .cuit("30444444440")
            .domicilio("Bv. Pellegrini 2500")
            .latitud(-31.6333)
            .longitud(-60.7)
            .telefono("342-4000000")
            .correoContacto("contacto@hotel.com")
            .categoria(4)
            .cerrado(false)
            .habitaciones(new ArrayList<>())
            .amenities(new ArrayList<>())
            .build();
    AmenityHotel wifi = new AmenityHotel();
    wifi.setAmenity(Amenity.WIFI);
    wifi.setHotel(nuevo);
    nuevo.getAmenities().add(wifi);
    hotel = hotelRepository.save(nuevo);

    reset(rabbitTemplate);
  }

  private TransactionTemplate tx() {
    return new TransactionTemplate(transactionManager);
  }

  private HabitacionRequest request(int numero, TipoHabitacion tipo) {
    return HabitacionRequest.builder()
        .numero(numero)
        .piso(1)
        .idTipoHabitacion(tipo.getId())
        .idHotel(hotel.getId())
        .build();
  }

  private Habitacion guardarHabitacion(int numero, TipoHabitacion tipo) {
    return habitacionRepository.save(
        Habitacion.builder()
            .numero(numero)
            .piso(1)
            .tipoHabitacion(tipo)
            .hotel(hotel)
            .disponible(true)
            .build());
  }

  private Tarifa tarifa(TipoHabitacion tipo, LocalDate inicio, LocalDate fin, double precio) {
    return tarifaRepository.save(
        Tarifa.builder()
            .tipoHabitacion(tipo)
            .fechaInicio(inicio)
            .fechaFin(fin)
            .precioNoche(precio)
            .build());
  }

  private List<HabitacionEvent> mensajesEnviados(int cantidad, String routingKey) throws Exception {
    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(rabbitTemplate, times(cantidad))
        .convertAndSend(eq("dan.exchange"), eq(routingKey), payload.capture());
    List<HabitacionEvent> eventos = new ArrayList<>();
    for (Object p : payload.getAllValues()) {
      eventos.add(objectMapper.readValue((String) p, HabitacionEvent.class));
    }
    return eventos;
  }

  // ------------------------------------------------- mensajes y transacciones

  @Test
  void crearHabitacion_envíaElMensajeCompletoDespuesDelCommit() throws Exception {
    tarifa(doble, LocalDate.now().minusDays(10), null, 15000.0);

    HabitacionResponse creada = habitacionService.crear(request(101, doble));

    HabitacionEvent evento = mensajesEnviados(1, "dan.habitacion.crear").get(0);
    assertEquals(TipoEvento.CREAR, evento.getTipoEvento());
    assertEquals(creada.id().longValue(), evento.getHabitacion().getHabitacionId());
    assertEquals(15000.0, evento.getHabitacion().getPrecioNoche());
    assertEquals(2, evento.getHabitacion().getCapacidad());
    assertEquals(List.of("WIFI"), evento.getHabitacion().getAmenities());
    assertEquals(-31.6333, evento.getHabitacion().getHotel().getLatitud());
    assertEquals(-60.7, evento.getHabitacion().getHotel().getLongitud());
  }

  @Test
  void crearHabitacion_siLaTransaccionHaceRollback_noSeEnviaNada() {
    tx().executeWithoutResult(
            status -> {
              habitacionService.crear(request(101, doble));
              status.setRollbackOnly();
            });

    verifyNoInteractions(rabbitTemplate);
    assertEquals(0, habitacionRepository.count());
  }

  @Test
  void eliminarHabitacion_envíaElMensajeDeBajaDespuesDelCommit() throws Exception {
    Habitacion h = guardarHabitacion(101, doble);

    habitacionService.eliminar(h.getId());

    HabitacionEvent evento = mensajesEnviados(1, "dan.habitacion.eliminar").get(0);
    assertEquals(TipoEvento.ELIMINAR, evento.getTipoEvento());
    assertEquals(h.getId().longValue(), evento.getHabitacion().getHabitacionId());
    assertFalse(habitacionRepository.existsById(h.getId()));
  }

  @Test
  void actualizarHotel_enviaUnMensajePorHabitacionDespuesDelCommit() throws Exception {
    guardarHabitacion(101, doble);
    guardarHabitacion(102, single);
    guardarHabitacion(103, doble);

    hotelService.actualizarHotel(
        hotel.getId(), new HotelUpdateRequest(5, "999", "nuevo@hotel.com"));

    List<HabitacionEvent> eventos = mensajesEnviados(3, "dan.habitacion.actualizar");
    eventos.forEach(
        e -> {
          assertEquals(TipoEvento.ACTUALIZAR_DATOS, e.getTipoEvento());
          assertEquals("999", e.getHabitacion().getHotel().getTelefono());
          assertEquals(5, e.getHabitacion().getHotel().getCategoria());
        });
  }

  @Test
  void actualizarHotel_siLaTransaccionHaceRollback_noSeEnviaNada() {
    guardarHabitacion(101, doble);
    guardarHabitacion(102, single);

    tx().executeWithoutResult(
            status -> {
              hotelService.actualizarHotel(
                  hotel.getId(), new HotelUpdateRequest(5, "999", "nuevo@hotel.com"));
              status.setRollbackOnly();
            });

    verifyNoInteractions(rabbitTemplate);
    assertEquals(4, hotelRepository.findById(hotel.getId()).orElseThrow().getCategoria());
  }

  @Test
  void agregarYQuitarAmenities_refrescanLasHabitacionesDelHotel() throws Exception {
    guardarHabitacion(101, doble);
    guardarHabitacion(102, single);

    hotelService.agregarAmenities(
        hotel.getId(),
        List.of(new AmenityHotelRequest(Amenity.PILETA), new AmenityHotelRequest(Amenity.WIFI)));

    List<HabitacionEvent> tras = mensajesEnviados(2, "dan.habitacion.actualizar");
    tras.forEach(e -> assertEquals(List.of("WIFI", "PILETA"), e.getHabitacion().getAmenities()));

    reset(rabbitTemplate);
    Long idWifi =
        tx().execute(
                status ->
                    hotelRepository.findById(hotel.getId()).orElseThrow().getAmenities().stream()
                        .filter(a -> a.getAmenity() == Amenity.WIFI)
                        .findFirst()
                        .orElseThrow()
                        .getId());
    hotelService.eliminarAmenity(hotel.getId(), idWifi);

    mensajesEnviados(2, "dan.habitacion.actualizar")
        .forEach(e -> assertEquals(List.of("PILETA"), e.getHabitacion().getAmenities()));
  }

  @Test
  void cerrarHotel_dejaLasHabitacionesNoDisponiblesYEnviaUnSoloMensaje() throws Exception {
    Habitacion h1 = guardarHabitacion(101, doble);
    Habitacion h2 = guardarHabitacion(102, single);

    hotelService.cerrarHotel(hotel.getId());

    assertTrue(hotelRepository.findById(hotel.getId()).orElseThrow().getCerrado());
    assertTrue(
        habitacionRepository.findByHotelId(hotel.getId()).stream()
            .noneMatch(Habitacion::getDisponible));
    HabitacionEvent evento = mensajesEnviados(1, "dan.habitacion.cerrar-hotel").get(0);
    assertEquals(TipoEvento.CERRAR_HOTEL, evento.getTipoEvento());
    assertEquals(hotel.getId(), evento.getHotelId());
    assertEquals(
        List.of(h1.getId().longValue(), h2.getId().longValue()),
        evento.getHabitacionIds().stream().sorted().toList());
    assertEquals(LocalDate.now(), evento.getFechaCierre());

    assertThrows(IllegalStateException.class, () -> hotelService.cerrarHotel(hotel.getId()));
    assertThrows(IllegalStateException.class, () -> habitacionService.crear(request(201, doble)));
  }

  // ---------------------------------------------------------------- tarifas

  @Test
  void obtenerTarifaVigente_ignoraVencidasYFuturasYAceptaFechaFinNula() {
    LocalDate hoy = LocalDate.now();
    tarifa(doble, hoy.minusDays(60), hoy.minusDays(31), 1000.0); // vencida
    tarifa(doble, hoy.minusDays(30), null, 15000.0); // vigente sin fin
    tarifa(doble, hoy.plusDays(30), null, 99999.0); // futura
    tarifa(single, hoy.minusDays(5), hoy.plusDays(5), 5000.0); // vigente con fin

    assertEquals(
        15000.0, tarifaService.obtenerTarifaVigente(doble.getId()).orElseThrow().getPrecioNoche());
    assertEquals(
        5000.0, tarifaService.obtenerTarifaVigente(single.getId()).orElseThrow().getPrecioNoche());
    assertTrue(tarifaService.obtenerTarifaVigente(99).isEmpty());
  }

  @Test
  void obtenerTarifaVigente_deUnaHabitacion_usaSuTipo() {
    tarifa(doble, LocalDate.now().minusDays(1), null, 15000.0);
    Habitacion h = guardarHabitacion(101, doble);

    assertEquals(15000.0, habitacionService.obtenerTarifaVigente(h.getId()).precioNoche());
  }

  // ---------------------------------------------------------------- filtros

  @Test
  void buscar_combinaLosFiltrosOpcionalesConAnd() {
    LocalDate hoy = LocalDate.now();
    tarifa(doble, hoy.minusDays(30), null, 15000.0);
    tarifa(single, hoy.minusDays(30), null, 5000.0);
    Habitacion rDoble = guardarHabitacion(101, doble);
    Habitacion rSingle = guardarHabitacion(102, single);

    assertEquals(2, habitacionService.buscar(null, null, null, null).size());
    assertEquals(List.of(rDoble.getId()), ids(habitacionService.buscar(2, null, null, null)));
    assertEquals(2, habitacionService.buscar(1, null, null, null).size());
    assertEquals(
        List.of(rSingle.getId()), ids(habitacionService.buscar(null, single.getId(), null, null)));
    assertEquals(List.of(rDoble.getId()), ids(habitacionService.buscar(null, null, 10000.0, null)));
    assertEquals(
        List.of(rSingle.getId()), ids(habitacionService.buscar(null, null, null, 10000.0)));
    assertEquals(List.of(rSingle.getId()), ids(habitacionService.buscar(1, null, 4000.0, 6000.0)));
    assertEquals(
        List.of(rDoble.getId()), ids(habitacionService.buscar(2, doble.getId(), 15000.0, 15000.0)));
    assertTrue(habitacionService.buscar(2, single.getId(), null, null).isEmpty());
    assertTrue(habitacionService.buscar(null, null, 20000.0, null).isEmpty());
  }

  @Test
  void buscar_porPrecioNoConsideraTarifasVencidas() {
    LocalDate hoy = LocalDate.now();
    tarifa(doble, hoy.minusDays(60), hoy.minusDays(31), 1000.0); // vencida, barata
    tarifa(doble, hoy.minusDays(30), null, 15000.0);
    guardarHabitacion(101, doble);

    assertTrue(habitacionService.buscar(null, null, null, 2000.0).isEmpty());
  }

  private static List<Integer> ids(List<HabitacionResponse> habitaciones) {
    return habitaciones.stream().map(HabitacionResponse::id).toList();
  }

  // ------------------------------------------------------- número único

  @Test
  void numeroDeHabitacion_noSePuedeRepetirEnElMismoHotel() {
    habitacionService.crear(request(101, doble));
    reset(rabbitTemplate);

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class, () -> habitacionService.crear(request(101, single)));
    assertTrue(ex.getMessage().contains("101"));
    assertEquals(1, habitacionRepository.count());
    verifyNoInteractions(rabbitTemplate);
  }

  @Test
  void numeroDeHabitacion_actualizarAUnoYaUsadoDaConflictoPeroConservarElPropioNo() {
    HabitacionResponse a = habitacionService.crear(request(101, doble));
    habitacionService.crear(request(102, doble));

    assertThrows(
        IllegalStateException.class,
        () -> habitacionService.actualizar(a.id(), request(102, doble)));
    assertEquals(101, habitacionService.actualizar(a.id(), request(101, single)).numero());
  }

  @Test
  void numeroDeHabitacion_lasRestriccionesDeLaBaseTambienLoImpiden() {
    guardarHabitacion(101, doble);

    assertThrows(DataIntegrityViolationException.class, () -> guardarHabitacion(101, single));
  }

  @Test
  void numeroDeHabitacion_puedeRepetirseEnOtroHotel() {
    habitacionService.crear(request(101, doble));
    Hotel otro =
        hotelRepository.save(
            Hotel.builder()
                .nombre("Otro")
                .cuit("30444444441")
                .domicilio("Calle 1234")
                .latitud(-31.0)
                .longitud(-60.0)
                .cerrado(false)
                .amenities(new ArrayList<>())
                .build());

    HabitacionResponse r =
        habitacionService.crear(
            HabitacionRequest.builder()
                .numero(101)
                .piso(1)
                .idTipoHabitacion(doble.getId())
                .idHotel(otro.getId())
                .build());

    assertEquals(otro.getId(), r.idHotel());
  }
}
