package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaVigenteResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.HabitacionMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.HabitacionMapperImpl;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventFactory;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

  @Mock private HabitacionRepository habitacionRepository;
  @Mock private HotelRepository hotelRepository;
  @Mock private TipoHabitacionRepository tipoHabitacionRepository;
  @Mock private TarifaService tarifaService;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Spy private HabitacionMapper habitacionMapper = new HabitacionMapperImpl();

  @InjectMocks private HabitacionService habitacionService;

  private Hotel hotel;
  private TipoHabitacion tipo;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        habitacionService, "eventFactory", new HabitacionEventFactory(tarifaService));

    hotel =
        Hotel.builder()
            .id(1)
            .nombre("Hotel Test")
            .cuit("30444444440")
            .domicilio("Bv. Pellegrini 2500, Santa Fe")
            .latitud(-31.6333)
            .longitud(-60.7)
            .telefono("342-4000000")
            .correoContacto("contacto@hoteltest.com")
            .categoria(4)
            .cerrado(false)
            .amenities(new ArrayList<>())
            .build();
    AmenityHotel wifi = new AmenityHotel();
    wifi.setAmenity(Amenity.WIFI);
    wifi.setHotel(hotel);
    AmenityHotel pileta = new AmenityHotel();
    pileta.setAmenity(Amenity.PILETA);
    pileta.setHotel(hotel);
    hotel.getAmenities().addAll(List.of(wifi, pileta));

    tipo = new TipoHabitacion(3, "DOBLE", "Doble una cama doble", 2);
  }

  private HabitacionRequest request(int numero) {
    return HabitacionRequest.builder()
        .numero(numero)
        .piso(2)
        .idTipoHabitacion(3)
        .idHotel(1)
        .build();
  }

  private Habitacion existente(int id, int numero) {
    return Habitacion.builder()
        .id(id)
        .numero(numero)
        .piso(2)
        .disponible(true)
        .tipoHabitacion(tipo)
        .hotel(hotel)
        .build();
  }

  private HabitacionEvent eventoPublicado() {
    ArgumentCaptor<HabitacionEventoPublicado> captor =
        ArgumentCaptor.forClass(HabitacionEventoPublicado.class);
    verify(eventPublisher).publishEvent(captor.capture());
    return captor.getValue().evento();
  }

  private void stubGuardar() {
    when(habitacionRepository.save(any(Habitacion.class)))
        .thenAnswer(
            inv -> {
              Habitacion h = inv.getArgument(0);
              if (h.getId() == null) {
                h.setId(10);
              }
              return h;
            });
  }

  // ------------------------------------------------------------------ crear

  @Test
  void crear_guardaYPublicaEventoCrearConTodosLosDatos() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(tarifaService.obtenerTarifaVigente(3))
        .thenReturn(Optional.of(Tarifa.builder().id(7).precioNoche(15000.0).build()));
    stubGuardar();

    HabitacionResponse response = habitacionService.crear(request(101));

    assertEquals(10, response.id());
    assertEquals(101, response.numero());
    assertEquals(true, response.disponible());
    assertEquals(1, response.idHotel());
    assertEquals(3, response.idTipoHabitacion());
    assertEquals("DOBLE", response.tipoHabitacion());

    HabitacionEvent evento = eventoPublicado();
    assertEquals(TipoEvento.CREAR, evento.getTipoEvento());
    HabitacionDTO dto = evento.getHabitacion();
    assertEquals(10L, dto.getHabitacionId());
    assertEquals(101, dto.getNumero());
    assertEquals(2, dto.getPiso());
    assertEquals(true, dto.getDisponible());
    assertEquals(3, dto.getTipoHabitacionId());
    assertEquals("DOBLE", dto.getTipoHabitacion());
    assertEquals("Doble una cama doble", dto.getTipoHabitacionDescripcion());
    assertEquals(2, dto.getCapacidad());
    assertEquals(15000.0, dto.getPrecioNoche());
    assertEquals(List.of("WIFI", "PILETA"), dto.getAmenities());
    HotelDTO hotelDto = dto.getHotel();
    assertEquals(1, hotelDto.getId());
    assertEquals("Hotel Test", hotelDto.getNombre());
    assertEquals("30444444440", hotelDto.getCuit());
    assertEquals("Bv. Pellegrini 2500, Santa Fe", hotelDto.getDomicilio());
    assertEquals(-31.6333, hotelDto.getLatitud());
    assertEquals(-60.7, hotelDto.getLongitud());
    assertEquals("342-4000000", hotelDto.getTelefono());
    assertEquals("contacto@hoteltest.com", hotelDto.getCorreoContacto());
    assertEquals(4, hotelDto.getCategoria());
  }

  @Test
  void crear_sinTarifaVigente_publicaPrecioNuloSinFallar() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(tarifaService.obtenerTarifaVigente(3)).thenReturn(Optional.empty());
    stubGuardar();

    habitacionService.crear(request(101));

    assertNull(eventoPublicado().getHabitacion().getPrecioNoche());
  }

  @Test
  void crear_disponibleNoInformadoQuedaDisponible() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(tarifaService.obtenerTarifaVigente(3)).thenReturn(Optional.empty());
    stubGuardar();

    assertTrue(habitacionService.crear(request(101)).disponible());
  }

  @Test
  void crear_enHotelCerrado_lanzaIllegalStateYNoPublica() {
    hotel.setCerrado(true);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));

    assertThrows(IllegalStateException.class, () -> habitacionService.crear(request(101)));
    verify(habitacionRepository, never()).save(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void crear_hotelInexistente_lanzaEntityNotFound() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> habitacionService.crear(request(101)));
  }

  @Test
  void crear_tipoInexistente_lanzaEntityNotFound() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> habitacionService.crear(request(101)));
  }

  @Test
  void crear_conNumeroRepetidoEnElMismoHotel_lanzaIllegalState() {
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(habitacionRepository.existsByHotelIdAndNumero(1, 101)).thenReturn(true);

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> habitacionService.crear(request(101)));
    assertTrue(ex.getMessage().contains("101"));
    verify(habitacionRepository, never()).save(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void crear_mismoNumeroEnOtroHotel_esValido() {
    Hotel otro = Hotel.builder().id(2).cerrado(false).amenities(new ArrayList<>()).build();
    when(hotelRepository.findById(2)).thenReturn(Optional.of(otro));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    // el 101 existe en el hotel 1 pero no en el 2
    when(habitacionRepository.existsByHotelIdAndNumero(2, 101)).thenReturn(false);
    when(tarifaService.obtenerTarifaVigente(3)).thenReturn(Optional.empty());
    stubGuardar();

    HabitacionResponse response =
        habitacionService.crear(
            HabitacionRequest.builder().numero(101).piso(1).idTipoHabitacion(3).idHotel(2).build());

    assertEquals(2, response.idHotel());
    verify(habitacionRepository).existsByHotelIdAndNumero(2, 101);
  }

  // ------------------------------------------------------------- actualizar

  @Test
  void actualizar_guardaYPublicaActualizarDatos() {
    Habitacion h = existente(10, 101);
    TipoHabitacion triple = new TipoHabitacion(5, "TRIPLE", "Triple", 3);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    when(tipoHabitacionRepository.findById(5)).thenReturn(Optional.of(triple));
    when(tarifaService.obtenerTarifaVigente(5))
        .thenReturn(Optional.of(Tarifa.builder().precioNoche(20000.0).build()));
    stubGuardar();

    HabitacionResponse response =
        habitacionService.actualizar(
            10,
            HabitacionRequest.builder()
                .numero(102)
                .piso(3)
                .idTipoHabitacion(5)
                .idHotel(1)
                .disponible(false)
                .build());

    assertEquals(102, response.numero());
    assertEquals(3, response.piso());
    assertEquals(5, response.idTipoHabitacion());
    assertEquals(false, response.disponible());
    HabitacionEvent evento = eventoPublicado();
    assertEquals(TipoEvento.ACTUALIZAR_DATOS, evento.getTipoEvento());
    assertEquals(10L, evento.getHabitacion().getHabitacionId());
    assertEquals(3, evento.getHabitacion().getCapacidad());
    assertEquals(20000.0, evento.getHabitacion().getPrecioNoche());
    assertEquals(false, evento.getHabitacion().getDisponible());
  }

  @Test
  void actualizar_numeroUsadoPorOtraHabitacionDelHotel_lanzaIllegalState() {
    Habitacion h = existente(10, 101);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(habitacionRepository.existsByHotelIdAndNumeroAndIdNot(1, 205, 10)).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> habitacionService.actualizar(10, request(205)));
    verify(habitacionRepository, never()).save(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void actualizar_conservandoSuPropioNumero_esValido() {
    Habitacion h = existente(10, 101);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    when(tipoHabitacionRepository.findById(3)).thenReturn(Optional.of(tipo));
    when(habitacionRepository.existsByHotelIdAndNumeroAndIdNot(1, 101, 10)).thenReturn(false);
    when(tarifaService.obtenerTarifaVigente(3)).thenReturn(Optional.empty());
    stubGuardar();

    assertEquals(101, habitacionService.actualizar(10, request(101)).numero());
  }

  @Test
  void actualizar_intentandoCambiarDeHotel_lanzaIllegalArgument() {
    Habitacion h = existente(10, 101);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    HabitacionRequest otroHotel =
        HabitacionRequest.builder().numero(101).piso(2).idTipoHabitacion(3).idHotel(2).build();

    assertThrows(IllegalArgumentException.class, () -> habitacionService.actualizar(10, otroHotel));
  }

  @Test
  void actualizar_habilitarDisponibleEnHotelCerrado_lanzaIllegalState() {
    hotel.setCerrado(true);
    Habitacion h = existente(10, 101);
    h.setDisponible(false);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    HabitacionRequest request =
        HabitacionRequest.builder()
            .numero(101)
            .piso(2)
            .idTipoHabitacion(3)
            .idHotel(1)
            .disponible(true)
            .build();

    assertThrows(IllegalStateException.class, () -> habitacionService.actualizar(10, request));
  }

  @Test
  void actualizar_inexistente_lanzaEntityNotFound() {
    when(habitacionRepository.findById(99)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class, () -> habitacionService.actualizar(99, request(101)));
  }

  // --------------------------------------------------------------- eliminar

  @Test
  void eliminar_borraYPublicaEventoEliminar() {
    Habitacion h = existente(10, 101);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));

    habitacionService.eliminar(10);

    verify(habitacionRepository).delete(h);
    HabitacionEvent evento = eventoPublicado();
    assertEquals(TipoEvento.ELIMINAR, evento.getTipoEvento());
    assertEquals(10L, evento.getHabitacion().getHabitacionId());
    assertEquals(1, evento.getHotelId());
  }

  @Test
  void eliminar_inexistente_lanzaEntityNotFound() {
    when(habitacionRepository.findById(99)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> habitacionService.eliminar(99));
    verifyNoInteractions(eventPublisher);
  }

  // ------------------------------------------------------------ consultas

  @Test
  @SuppressWarnings("unchecked")
  void buscar_conFiltros_usaUnaSpecificationYMapeaLaRespuesta() {
    when(habitacionRepository.findAll(any(Specification.class)))
        .thenReturn(List.of(existente(10, 101)));

    List<HabitacionResponse> out = habitacionService.buscar(2, 3, 1000.0, 20000.0);

    assertEquals(1, out.size());
    assertEquals(101, out.get(0).numero());
  }

  @Test
  void buscar_precioMinMayorQuePrecioMax_lanzaIllegalArgument() {
    assertThrows(
        IllegalArgumentException.class, () -> habitacionService.buscar(null, null, 5000.0, 1000.0));
  }

  @Test
  void obtenerTarifaVigente_devuelveLaTarifaDelTipoDeLaHabitacion() {
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(existente(10, 101)));
    when(tarifaService.obtenerTarifaVigente(3))
        .thenReturn(
            Optional.of(
                Tarifa.builder()
                    .id(7)
                    .fechaInicio(LocalDate.of(2026, 1, 1))
                    .precioNoche(15000.0)
                    .tipoHabitacion(tipo)
                    .build()));

    TarifaVigenteResponse out = habitacionService.obtenerTarifaVigente(10);

    assertEquals(7, out.id());
    assertEquals(3, out.idTipoHabitacion());
    assertEquals(15000.0, out.precioNoche());
    assertNull(out.fechaFin());
  }

  @Test
  void obtenerTarifaVigente_sinTarifa_lanzaEntityNotFound() {
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(existente(10, 101)));
    when(tarifaService.obtenerTarifaVigente(3)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> habitacionService.obtenerTarifaVigente(10));
  }

  @Test
  void estaDisponible_reflejaElAtributoDeLaHabitacion() {
    Habitacion h = existente(10, 101);
    when(habitacionRepository.findById(10)).thenReturn(Optional.of(h));
    assertTrue(habitacionService.estaDisponible(10));

    h.setDisponible(false);
    assertFalse(habitacionService.estaDisponible(10));
  }
}
