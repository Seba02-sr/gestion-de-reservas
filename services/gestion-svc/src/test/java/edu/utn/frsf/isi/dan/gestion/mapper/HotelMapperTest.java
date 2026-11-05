package edu.utn.frsf.isi.dan.gestion.mapper;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.gestion.dto.HotelUpdateRequest;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HotelMapperTest {

  private final HotelMapper mapper = new HotelMapperImpl();

  @Test
  void updateEntityFromRequest_soloCambiaCategoriaTelefonoYCorreo() {
    List<Habitacion> habitaciones = new ArrayList<>();
    Hotel hotel =
        Hotel.builder()
            .id(1)
            .nombre("Hotel Test")
            .cuit("30444444440")
            .domicilio("Calle Falsa 123")
            .latitud(-31.6)
            .longitud(-60.7)
            .telefono("111")
            .correoContacto("viejo@hotel.com")
            .categoria(3)
            .cerrado(false)
            .habitaciones(habitaciones)
            .amenities(new ArrayList<>())
            .build();

    mapper.updateEntityFromRequest(new HotelUpdateRequest(5, "222", "nuevo@hotel.com"), hotel);

    assertEquals(5, hotel.getCategoria());
    assertEquals("222", hotel.getTelefono());
    assertEquals("nuevo@hotel.com", hotel.getCorreoContacto());
    assertEquals(1, hotel.getId());
    assertEquals("Hotel Test", hotel.getNombre());
    assertEquals("30444444440", hotel.getCuit());
    assertEquals("Calle Falsa 123", hotel.getDomicilio());
    assertEquals(-31.6, hotel.getLatitud());
    assertEquals(-60.7, hotel.getLongitud());
    assertFalse(hotel.getCerrado());
    assertSame(habitaciones, hotel.getHabitaciones());
  }
}
