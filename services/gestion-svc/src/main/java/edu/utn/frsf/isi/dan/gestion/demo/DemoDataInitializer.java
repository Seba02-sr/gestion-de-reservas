package edu.utn.frsf.isi.dan.gestion.demo;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.demo.DemoCatalogo.Hab;
import edu.utn.frsf.isi.dan.gestion.demo.DemoCatalogo.HotelDemo;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.service.HabitacionService;
import edu.utn.frsf.isi.dan.gestion.service.HotelService;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga datos de demo (hoteles, amenities, tarifas y habitaciones) al arrancar, para que quien
 * corrige encuentre resultados al abrir el front.
 *
 * <ul>
 *   <li><b>Desactivable</b>: solo existe con {@code demo.datos.habilitado=true} (variable {@code
 *       DEMO_DATOS_HABILITADO}; el compose la pone en true).
 *   <li><b>Idempotente</b>: cada paso se saltea si el dato ya existe (hotel por CUIT, tarifa si el
 *       tipo ya tiene alguna, habitación por hotel+número, amenities ignora duplicados).
 *   <li><b>Pasa por los services</b> (no por los repositorios): así cada alta publica su evento de
 *       RabbitMQ (AFTER_COMMIT) y reservas-svc replica las habitaciones como con cualquier alta.
 *       Nada se escribe directo en MongoDB.
 * </ul>
 *
 * Orden: hotel → amenities → tarifas → habitaciones. Las tarifas van antes de las habitaciones para
 * que el evento CREAR ya lleve el precio vigente; los amenities antes de las habitaciones porque un
 * ACTUALIZAR_DATOS que llegue antes del CREAR se descarta en reservas-svc.
 */
@Component
@Order(10)
@Log4j2
@ConditionalOnProperty(name = "demo.datos.habilitado", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

  private final HotelService hotelService;
  private final TarifaService tarifaService;
  private final HabitacionService habitacionService;
  private final HabitacionRepository habitacionRepository;
  private final Validator validator;

  public DemoDataInitializer(
      HotelService hotelService,
      TarifaService tarifaService,
      HabitacionService habitacionService,
      HabitacionRepository habitacionRepository,
      Validator validator) {
    this.hotelService = hotelService;
    this.tarifaService = tarifaService;
    this.habitacionService = habitacionService;
    this.habitacionRepository = habitacionRepository;
    this.validator = validator;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("Datos de demo: verificando hoteles, tarifas y habitaciones...");
    cargarTarifas();
    int hoteles = 0;
    for (HotelDemo demo : DemoCatalogo.HOTELES) {
      try {
        cargarHotel(demo);
        hoteles++;
      } catch (RuntimeException e) {
        // un hotel con problemas no impide cargar los demás ni arrancar la aplicación
        log.error("Datos de demo: no se pudo cargar '{}': {}", demo.nombre(), e.getMessage(), e);
      }
    }
    log.info("Datos de demo: {} de {} hoteles verificados.", hoteles, DemoCatalogo.HOTELES.size());
  }

  private void cargarTarifas() {
    DemoCatalogo.TARIFAS_POR_TIPO.forEach(
        (tipo, precio) -> {
          try {
            if (tarifaService.listar(tipo).isEmpty()) {
              tarifaService.crear(
                  TarifaRequest.builder()
                      .nombre("Tarifa demo tipo " + tipo)
                      .idTipoHabitacion(tipo)
                      .precioNoche(precio)
                      .build());
              log.info("Datos de demo: tarifa creada para el tipo {} ({} ARS)", tipo, precio);
            }
          } catch (RuntimeException e) {
            log.error(
                "Datos de demo: no se pudo crear la tarifa del tipo {}: {}", tipo, e.getMessage());
          }
        });
  }

  private void cargarHotel(HotelDemo demo) {
    String cuit = Cuit.completar(demo.cuitBase());
    Integer idHotel =
        hotelService.consultarHoteles(null, cuit, null, null, null).stream()
            .findFirst()
            .map(h -> h.id())
            .orElse(null);

    if (idHotel == null) {
      HotelRequest request =
          HotelRequest.builder()
              .nombre(demo.nombre())
              .cuit(cuit)
              .domicilio(demo.domicilio())
              .latitud(demo.latitud())
              .longitud(demo.longitud())
              .telefono(demo.telefono())
              .correoContacto(demo.correo())
              .categoria(demo.categoria())
              .build();
      Set<ConstraintViolation<HotelRequest>> errores = validator.validate(request);
      if (!errores.isEmpty()) {
        throw new ConstraintViolationException(errores);
      }
      idHotel = hotelService.crearHotel(request).id();
      log.info("Datos de demo: hotel creado '{}' (id {})", demo.nombre(), idHotel);
    }

    // Antes de las habitaciones; ignora los amenities que el hotel ya tiene (idempotente)
    List<AmenityHotelRequest> amenities =
        demo.amenities().stream().map(AmenityHotelRequest::new).toList();
    hotelService.agregarAmenities(idHotel, amenities);

    for (Hab hab : demo.habitaciones()) {
      if (habitacionRepository.existsByHotelIdAndNumero(idHotel, hab.numero())) {
        continue;
      }
      habitacionService.crear(
          HabitacionRequest.builder()
              .numero(hab.numero())
              .piso(hab.piso())
              .idTipoHabitacion(hab.tipo())
              .idHotel(idHotel)
              .disponible(true)
              .build());
    }
  }
}
