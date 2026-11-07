package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.dto.HotelUpdateRequest;
import edu.utn.frsf.isi.dan.gestion.mapper.AmenityHotelMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventFactory;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

  @Mock private HotelRepository hotelRepository;

  @Mock private HotelMapper hotelMapper;

  @Mock private AmenityHotelMapper amenityHotelMapper;

  @Mock private HabitacionRepository habitacionRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private TarifaService tarifaService;

  @InjectMocks private HotelService hotelService;

  private HotelRequest hotelRequest;
  private Hotel hotel;
  private HotelResponse hotelResponse;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        hotelService, "eventFactory", new HabitacionEventFactory(tarifaService));
    hotelRequest =
        new HotelRequest(
            "Hotel Test",
            "30444444440",
            "Calle Falsa 123",
            31.6107,
            60.6735,
            "123456789",
            "contacto@hoteltest.com",
            5);

    hotel = new Hotel();
    hotel.setId(1);
    hotel.setNombre("Hotel Test");
    hotel.setCuit("30444444440");
    hotel.setDomicilio("Calle Falsa 123");
    hotel.setLatitud(31.6107);
    hotel.setLongitud(60.6735);
    hotel.setCategoria(5);
    hotel.setTelefono("123456789");
    hotel.setCorreoContacto("contacto@hoteltest.com");

    hotelResponse =
        new HotelResponse(
            1,
            "Hotel Test",
            "30444444440",
            "Calle Falsa 123",
            31.6107,
            60.6735,
            "123456789",
            "contacto@hoteltest.com",
            5,
            false);
  }

  @Test
  void crearHotel_cuandoRequestEsNulo_debeLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.crearHotel(null);
        });
  }

  @Test
  void crearHotel_cuandoRequestEsValido_debeCrearHotel() {
    when(hotelMapper.toEntity(any(HotelRequest.class))).thenReturn(hotel);
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    HotelResponse result = hotelService.crearHotel(hotelRequest);

    assertNotNull(result);
    assertEquals(hotelResponse, result);
    verify(hotelRepository, times(1)).save(hotel);
  }

  @Test
  void crearHotel_cuandoSaveFalla_debeLanzarExcepcion() {
    when(hotelMapper.toEntity(any(HotelRequest.class))).thenReturn(hotel);
    when(hotelRepository.save(any(Hotel.class))).thenThrow(new RuntimeException("Error de DB"));

    assertThrows(
        RuntimeException.class,
        () -> {
          hotelService.crearHotel(hotelRequest);
        });
  }

  @Test
  void actualizarHotel_cuandoRequestEsNulo_debeLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.actualizarHotel(1, null);
        });
  }

  @Test
  void actualizarHotel_cuandoHotelNoExiste_debeLanzarEntityNotFound() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    HotelUpdateRequest request = new HotelUpdateRequest(4, "987654321", "nuevo@hoteltest.com");
    assertThrows(EntityNotFoundException.class, () -> hotelService.actualizarHotel(1, request));
  }

  @Test
  void actualizarHotel_cuandoRequestEsValido_debeActualizarHotel() {
    HotelUpdateRequest requestValido =
        new HotelUpdateRequest(4, "987654321", "nuevo@hoteltest.com");
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    HotelResponse result = hotelService.actualizarHotel(1, requestValido);

    assertNotNull(result);
    verify(hotelMapper).updateEntityFromRequest(requestValido, hotel);
    verify(hotelRepository, times(1)).save(hotel);
  }

  @Test
  void actualizarHotel_publicaUnActualizarDatosPorCadaHabitacion() {
    hotel.setAmenities(new ArrayList<>(List.of(amenityHotel(1L, Amenity.WIFI))));
    Habitacion h1 = habitacion(10, 101);
    Habitacion h2 = habitacion(11, 102);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);
    when(habitacionRepository.findByHotelId(1)).thenReturn(List.of(h1, h2));
    // el mapper (mock) aplica el cambio como lo haría el real
    doAnswer(
            inv -> {
              HotelUpdateRequest req = inv.getArgument(0);
              Hotel h = inv.getArgument(1);
              h.setTelefono(req.telefono());
              h.setCategoria(req.categoria());
              return null;
            })
        .when(hotelMapper)
        .updateEntityFromRequest(any(HotelUpdateRequest.class), any(Hotel.class));

    hotelService.actualizarHotel(1, new HotelUpdateRequest(3, "555-0000", "nuevo@hoteltest.com"));

    List<HabitacionEvent> eventos = eventosPublicados(2);
    assertEquals(
        List.of(10L, 11L), eventos.stream().map(e -> e.getHabitacion().getHabitacionId()).toList());
    eventos.forEach(
        e -> {
          assertEquals(TipoEvento.ACTUALIZAR_DATOS, e.getTipoEvento());
          assertEquals("555-0000", e.getHabitacion().getHotel().getTelefono());
          assertEquals(3, e.getHabitacion().getHotel().getCategoria());
          assertEquals(31.6107, e.getHabitacion().getHotel().getLatitud());
          assertEquals(List.of("WIFI"), e.getHabitacion().getAmenities());
          assertEquals("DOBLE", e.getHabitacion().getTipoHabitacion());
          assertEquals(2, e.getHabitacion().getCapacidad());
        });
  }

  @Test
  void actualizarHotel_sinHabitaciones_noPublicaEventos() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);
    when(habitacionRepository.findByHotelId(1)).thenReturn(List.of());

    hotelService.actualizarHotel(1, new HotelUpdateRequest(3, "555-0000", "nuevo@hoteltest.com"));

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void obtenerHotel_cuandoExiste_devuelveElHotel() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelMapper.toResponse(hotel)).thenReturn(hotelResponse);

    assertEquals(hotelResponse, hotelService.obtenerHotel(1));
  }

  @Test
  void obtenerHotel_cuandoNoExiste_lanzaEntityNotFound() {
    when(hotelRepository.findById(9)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> hotelService.obtenerHotel(9));
  }

  @Test
  void cerrarHotel_cuandoIdEsNulo_debeLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.cerrarHotel(null);
        });
  }

  @Test
  void cerrarHotel_cuandoHotelNoExiste_debeLanzarEntityNotFound() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          hotelService.cerrarHotel(1);
        });
  }

  @Test
  void cerrarHotel_cuandoHotelExiste_debeMarcarloComoCerrado() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    HotelResponse result = hotelService.cerrarHotel(1);

    assertNotNull(result);
    assertTrue(hotel.getCerrado());
    verify(hotelRepository, times(1)).save(hotel);
  }

  @Test
  void cerrarHotel_dejaLasHabitacionesNoDisponiblesYPublicaCerrarHotel() {
    Habitacion h1 = habitacion(10, 101);
    Habitacion h2 = habitacion(11, 102);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);
    when(habitacionRepository.findByHotelId(1)).thenReturn(List.of(h1, h2));

    hotelService.cerrarHotel(1);

    assertFalse(h1.getDisponible());
    assertFalse(h2.getDisponible());
    verify(habitacionRepository).saveAll(List.of(h1, h2));
    HabitacionEvent evento = eventosPublicados(1).get(0);
    assertEquals(TipoEvento.CERRAR_HOTEL, evento.getTipoEvento());
    assertEquals(1, evento.getHotelId());
    assertEquals(List.of(10L, 11L), evento.getHabitacionIds());
    assertEquals(java.time.LocalDate.now(), evento.getFechaCierre());
  }

  @Test
  void cerrarHotel_cuandoYaEstabaCerrado_lanzaIllegalStateYNoPublica() {
    hotel.setCerrado(true);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));

    assertThrows(IllegalStateException.class, () -> hotelService.cerrarHotel(1));
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void agregarAmenities_cuandoRequestEsNuloOVacio_debeLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.agregarAmenities(1, null);
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.agregarAmenities(1, Collections.emptyList());
        });
  }

  @Test
  void agregarAmenities_cuandoHotelNoExiste_debeLanzarExcepcion() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          hotelService.agregarAmenities(1, List.of(new AmenityHotelRequest(Amenity.GIMNASIO)));
        });
  }

  @Test
  void agregarAmenities_cuandoRequestEsValido_debeAgregarAmenities() {
    AmenityHotelRequest amenityRequest = new AmenityHotelRequest(Amenity.GIMNASIO);
    AmenityHotel amenityHotel = amenityHotel(null, Amenity.GIMNASIO);
    hotel.setAmenities(new ArrayList<>()); // Inicializa la lista de amenities
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(amenityHotelMapper.toEntity(any(AmenityHotelRequest.class))).thenReturn(amenityHotel);
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    HotelResponse result = hotelService.agregarAmenities(1, List.of(amenityRequest));

    assertNotNull(result);
    verify(hotelRepository, times(1)).save(hotel);
    assertEquals(1, hotel.getAmenities().size());
    assertSame(hotel, amenityHotel.getHotel());
  }

  @Test
  void agregarAmenities_noDuplicaLaQueElHotelYaTieneNiLasRepetidas() {
    hotel.setAmenities(new ArrayList<>(List.of(amenityHotel(1L, Amenity.WIFI))));
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(amenityHotelMapper.toEntity(new AmenityHotelRequest(Amenity.PILETA)))
        .thenReturn(amenityHotel(null, Amenity.PILETA));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    hotelService.agregarAmenities(
        1,
        List.of(
            new AmenityHotelRequest(Amenity.WIFI),
            new AmenityHotelRequest(Amenity.PILETA),
            new AmenityHotelRequest(Amenity.PILETA)));

    assertEquals(
        List.of(Amenity.WIFI, Amenity.PILETA),
        hotel.getAmenities().stream().map(AmenityHotel::getAmenity).toList());
    verify(amenityHotelMapper, times(1)).toEntity(any(AmenityHotelRequest.class));
  }

  @Test
  void agregarAmenities_publicaUnActualizarDatosPorHabitacionConLasAmenitiesNuevas() {
    hotel.setAmenities(new ArrayList<>());
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(amenityHotelMapper.toEntity(any(AmenityHotelRequest.class)))
        .thenReturn(amenityHotel(null, Amenity.SPA));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);
    when(habitacionRepository.findByHotelId(1))
        .thenReturn(List.of(habitacion(10, 101), habitacion(11, 102)));

    hotelService.agregarAmenities(1, List.of(new AmenityHotelRequest(Amenity.SPA)));

    List<HabitacionEvent> eventos = eventosPublicados(2);
    eventos.forEach(
        e -> {
          assertEquals(TipoEvento.ACTUALIZAR_DATOS, e.getTipoEvento());
          assertEquals(List.of("SPA"), e.getHabitacion().getAmenities());
        });
  }

  @Test
  void agregarAmenities_cuandoNoHuboCambios_noPublicaEventos() {
    hotel.setAmenities(new ArrayList<>(List.of(amenityHotel(1L, Amenity.WIFI))));
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    hotelService.agregarAmenities(1, List.of(new AmenityHotelRequest(Amenity.WIFI)));

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void eliminarAmenity_cuandoIdEsNulo_debeLanzarExcepcion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.eliminarAmenity(null, 1L);
        });
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.eliminarAmenity(1, null);
        });
  }

  @Test
  void eliminarAmenity_cuandoHotelNoExiste_debeLanzarExcepcion() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          hotelService.eliminarAmenity(1, 1L);
        });
  }

  @Test
  void eliminarAmenity_cuandoAmenityNoExisteEnHotel_debeLanzarExcepcion() {
    hotel.setAmenities(new ArrayList<>()); // Inicializa la lista de amenities
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          hotelService.eliminarAmenity(1, 1L);
        });
  }

  @Test
  void eliminarAmenity_cuandoRequestEsValido_debeEliminarAmenity() {
    AmenityHotel amenityHotel = amenityHotel(1L, Amenity.GIMNASIO);
    hotel.setAmenities(new ArrayList<>(List.of(amenityHotel)));
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));

    hotelService.eliminarAmenity(1, 1L);

    verify(hotelRepository, times(1)).save(hotel);
    assertTrue(hotel.getAmenities().isEmpty());
  }

  @Test
  void eliminarAmenity_publicaUnActualizarDatosPorHabitacionSinLaAmenityQuitada() {
    hotel.setAmenities(
        new ArrayList<>(
            List.of(amenityHotel(1L, Amenity.GIMNASIO), amenityHotel(2L, Amenity.WIFI))));
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(habitacionRepository.findByHotelId(1))
        .thenReturn(List.of(habitacion(10, 101), habitacion(11, 102), habitacion(12, 103)));

    hotelService.eliminarAmenity(1, 1L);

    List<HabitacionEvent> eventos = eventosPublicados(3);
    eventos.forEach(
        e -> {
          assertEquals(TipoEvento.ACTUALIZAR_DATOS, e.getTipoEvento());
          assertEquals(List.of("WIFI"), e.getHabitacion().getAmenities());
        });
  }

  @Test
  void listarHoteles_debeRetornarListaDeHoteles() {
    when(hotelRepository.findAll()).thenReturn(List.of(hotel));
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    List<HotelResponse> result = hotelService.listarHoteles();

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    assertEquals(hotelResponse, result.get(0));
  }

  @Test
  void listarAmenities_cuandoHotelNoExiste_debeLanzarExcepcion() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          hotelService.listarAmenities(1);
        });
  }

  @Test
  void consultarHoteles_sinFiltros_debeRetornarTodos() {
    when(hotelRepository.findAll(any(Specification.class))).thenReturn(List.of(hotel));
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    List<HotelResponse> result = hotelService.consultarHoteles(null, null, null, null, null);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
  }

  @Test
  void consultarHoteles_conFiltroNombre_debeRetornarFiltrados() {
    when(hotelRepository.findAll(any(Specification.class))).thenReturn(List.of(hotel));
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    List<HotelResponse> result = hotelService.consultarHoteles("Test", null, null, null, null);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
  }

  // ---- helpers

  private static AmenityHotel amenityHotel(Long id, Amenity amenity) {
    AmenityHotel ah = new AmenityHotel();
    ah.setId(id);
    ah.setAmenity(amenity);
    return ah;
  }

  private Habitacion habitacion(int id, int numero) {
    TipoHabitacion tipo = new TipoHabitacion(3, "DOBLE", "Doble una cama doble", 2);
    return Habitacion.builder()
        .id(id)
        .numero(numero)
        .piso(1)
        .tipoHabitacion(tipo)
        .hotel(hotel)
        .build();
  }

  /** Captura los eventos publicados (esperando exactamente {@code cantidad}). */
  private List<HabitacionEvent> eventosPublicados(int cantidad) {
    ArgumentCaptor<HabitacionEventoPublicado> captor =
        ArgumentCaptor.forClass(HabitacionEventoPublicado.class);
    verify(eventPublisher, times(cantidad)).publishEvent(captor.capture());
    return captor.getAllValues().stream().map(HabitacionEventoPublicado::evento).toList();
  }
}
