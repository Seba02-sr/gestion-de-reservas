package edu.utn.frsf.isi.dan.gestion.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.demo.DemoCatalogo.HotelDemo;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.service.HabitacionService;
import edu.utn.frsf.isi.dan.gestion.service.HotelService;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoDataInitializerTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
  private final HotelService hotelService = mock(HotelService.class);
  private final TarifaService tarifaService = mock(TarifaService.class);
  private final HabitacionService habitacionService = mock(HabitacionService.class);
  private final HabitacionRepository habitacionRepository = mock(HabitacionRepository.class);
  private final DemoDataInitializer initializer =
      new DemoDataInitializer(
          hotelService, tarifaService, habitacionService, habitacionRepository, validator);

  private static HotelResponse hotel(int id) {
    return new HotelResponse(id, "h", "c", "d", 0.0, 0.0, "t", "m", 3, false);
  }

  private static int totalHabitaciones() {
    return DemoCatalogo.HOTELES.stream().mapToInt(h -> h.habitaciones().size()).sum();
  }

  @Test
  void loscuitsDeLosHotelesDemoSonValidosParaElValidadorDeLaAplicacion() {
    for (HotelDemo h : DemoCatalogo.HOTELES) {
      HotelRequest request =
          HotelRequest.builder()
              .nombre(h.nombre())
              .cuit(Cuit.completar(h.cuitBase()))
              .domicilio(h.domicilio())
              .latitud(h.latitud())
              .longitud(h.longitud())
              .telefono(h.telefono())
              .correoContacto(h.correo())
              .categoria(h.categoria())
              .build();
      assertTrue(validator.validate(request).isEmpty(), "inválido: " + h.nombre());
    }
  }

  @Test
  void elCatalogoCubreSeisCiudadesConVariedadDeCategoriasYTipos() {
    assertEquals(6, DemoCatalogo.HOTELES.size());
    long categorias = DemoCatalogo.HOTELES.stream().map(HotelDemo::categoria).distinct().count();
    assertTrue(categorias >= 3);
    long cuits =
        DemoCatalogo.HOTELES.stream().map(h -> Cuit.completar(h.cuitBase())).distinct().count();
    assertEquals(6, cuits);
    // todos los tipos usados tienen tarifa
    DemoCatalogo.HOTELES.forEach(
        h ->
            h.habitaciones()
                .forEach(hab -> assertTrue(DemoCatalogo.TARIFAS_POR_TIPO.containsKey(hab.tipo()))));
  }

  @Test
  void conLaBaseVaciaCreaTodoPorLosServices() {
    when(tarifaService.listar(anyInt())).thenReturn(List.of());
    when(hotelService.consultarHoteles(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(hotelService.crearHotel(any())).thenReturn(hotel(1));
    when(habitacionRepository.existsByHotelIdAndNumero(anyInt(), anyInt())).thenReturn(false);

    initializer.run(null);

    verify(tarifaService, times(DemoCatalogo.TARIFAS_POR_TIPO.size()))
        .crear(any(TarifaRequest.class));
    verify(hotelService, times(6)).crearHotel(any(HotelRequest.class));
    verify(hotelService, times(6)).agregarAmenities(eq(1), any());
    verify(habitacionService, times(totalHabitaciones())).crear(any(HabitacionRequest.class));
  }

  @Test
  void elOrdenEsTarifasAntesQueHabitaciones_yAmenitiesAntesQueHabitaciones() {
    when(tarifaService.listar(anyInt())).thenReturn(List.of());
    when(hotelService.consultarHoteles(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(hotelService.crearHotel(any())).thenReturn(hotel(1));

    initializer.run(null);

    var orden = org.mockito.Mockito.inOrder(tarifaService, hotelService, habitacionService);
    orden.verify(tarifaService, org.mockito.Mockito.atLeastOnce()).crear(any());
    orden.verify(hotelService).crearHotel(any());
    orden.verify(hotelService).agregarAmenities(anyInt(), any());
    orden.verify(habitacionService, org.mockito.Mockito.atLeastOnce()).crear(any());
  }

  @Test
  void laSegundaEjecucionNoCreaNada_idempotente() {
    when(tarifaService.listar(anyInt())).thenReturn(List.of(mock(TarifaResponse.class)));
    when(hotelService.consultarHoteles(any(), any(), any(), any(), any()))
        .thenReturn(List.of(hotel(5)));
    when(habitacionRepository.existsByHotelIdAndNumero(anyInt(), anyInt())).thenReturn(true);

    initializer.run(null);

    verify(tarifaService, never()).crear(any());
    verify(hotelService, never()).crearHotel(any());
    verify(habitacionService, never()).crear(any());
  }

  @Test
  void unHotelConError_noImpideCargarLosDemas() {
    when(tarifaService.listar(anyInt())).thenReturn(List.of());
    when(hotelService.consultarHoteles(any(), any(), any(), any(), any())).thenReturn(List.of());
    when(hotelService.crearHotel(any()))
        .thenThrow(new IllegalStateException("falla"))
        .thenReturn(hotel(2));

    initializer.run(null);

    verify(hotelService, times(6)).crearHotel(any());
    ArgumentCaptor<HabitacionRequest> hab = ArgumentCaptor.forClass(HabitacionRequest.class);
    verify(habitacionService, org.mockito.Mockito.atLeastOnce()).crear(hab.capture());
    assertTrue(hab.getAllValues().stream().allMatch(h -> h.idHotel() == 2));
  }

  @Test
  void soloExisteConLaPropiedadDemoDatosHabilitado() {
    ApplicationContextRunner base =
        new ApplicationContextRunner()
            .withInitializer(
                ctx -> {
                  // singletons ya construidos: Spring no les inyecta campos (son mocks)
                  var bf = ctx.getBeanFactory();
                  bf.registerSingleton("hotelService", hotelService);
                  bf.registerSingleton("tarifaService", tarifaService);
                  bf.registerSingleton("habitacionService", habitacionService);
                  bf.registerSingleton("habitacionRepository", habitacionRepository);
                  bf.registerSingleton("validator", validator);
                })
            .withConfiguration(UserConfigurations.of(DemoDataInitializer.class));

    base.run(ctx -> assertTrue(ctx.getBeansOfType(DemoDataInitializer.class).isEmpty()));
    base.withPropertyValues("demo.datos.habilitado=false")
        .run(ctx -> assertTrue(ctx.getBeansOfType(DemoDataInitializer.class).isEmpty()));
    base.withPropertyValues("demo.datos.habilitado=true")
        .run(ctx -> assertEquals(1, ctx.getBeansOfType(DemoDataInitializer.class).size()));
  }
}
