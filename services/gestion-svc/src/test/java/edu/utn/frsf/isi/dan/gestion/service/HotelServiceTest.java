
package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.mapper.AmenityHotelMapper;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

  @Mock private HotelRepository hotelRepository;

  @Mock private HotelMapper hotelMapper;

  @Mock private AmenityHotelMapper amenityHotelMapper;

  @InjectMocks private HotelService hotelService;

  private HotelRequest hotelRequest;
  private Hotel hotel;
  private HotelResponse hotelResponse;

  @BeforeEach
  void setUp() {
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
  void actualizarHotel_cuandoHotelNoExiste_debeLanzarExcepcion() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.actualizarHotel(1, hotelRequest);
        });
  }

  @Test
  void actualizarHotel_cuandoIntentaModificarCamposNoPermitidos_debeLanzarExcepcion() {
    HotelRequest requestModificado =
        new HotelRequest(
            "Otro Nombre",
            "30444444440",
            "Otra Calle 456",
            32.6107,
            61.6735,
            "987654321",
            "otro@hoteltest.com",
            4);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.actualizarHotel(1, requestModificado);
        });
  }

  @Test
  void actualizarHotel_cuandoRequestEsValido_debeActualizarHotel() {
    HotelRequest requestValido =
        new HotelRequest(
            "Hotel Test",
            "30444444440",
            "Calle Falsa 123",
            31.6107,
            60.6735,
            "987654321",
            "nuevo@hoteltest.com",
            5);
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
    when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

    HotelResponse result = hotelService.actualizarHotel(1, requestValido);

    assertNotNull(result);
    verify(hotelRepository, times(1)).save(hotel);
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
  void cerrarHotel_cuandoHotelNoExiste_debeLanzarExcepcion() {
    when(hotelRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(
        IllegalArgumentException.class,
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
        IllegalArgumentException.class,
        () -> {
          hotelService.agregarAmenities(1, List.of(new AmenityHotelRequest(1, Amenity.GIMNASIO)));
        });
  }

  @Test
  void agregarAmenities_cuandoRequestEsValido_debeAgregarAmenities() {
  AmenityHotelRequest amenityRequest = new AmenityHotelRequest(1, Amenity.GIMNASIO);
  AmenityHotel amenityHotel = new AmenityHotel();
  hotel.setAmenities(new ArrayList<>()); // Inicializa la lista de amenities
  when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
  when(amenityHotelMapper.toEntity(any(AmenityHotelRequest.class))).thenReturn(amenityHotel);
  when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);
  when(hotelMapper.toResponse(any(Hotel.class))).thenReturn(hotelResponse);

  HotelResponse result = hotelService.agregarAmenities(1, List.of(amenityRequest));

  assertNotNull(result);
  verify(hotelRepository, times(1)).save(hotel);
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
        IllegalArgumentException.class,
        () -> {
          hotelService.eliminarAmenity(1, 1L);
        });
  }

  @Test
  void eliminarAmenity_cuandoAmenityNoExisteEnHotel_debeLanzarExcepcion() {
    hotel.setAmenities(new ArrayList<>()); // Inicializa la lista de amenities
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          hotelService.eliminarAmenity(1, 1L);
        });
  }

  @Test
  void eliminarAmenity_cuandoRequestEsValido_debeEliminarAmenity() {
    AmenityHotel amenityHotel = new AmenityHotel();
    amenityHotel.setId(1L);
    hotel.setAmenities(new ArrayList<>(List.of(amenityHotel)));
    when(hotelRepository.findById(1)).thenReturn(Optional.of(hotel));

    hotelService.eliminarAmenity(1, 1L);

    verify(hotelRepository, times(1)).save(hotel);
    assertTrue(hotel.getAmenities().isEmpty());
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
        IllegalArgumentException.class,
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
}
