package edu.utn.frsf.isi.dan.gestion.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.gestion.scheduling.TarifaPrecioScheduler;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import edu.utn.frsf.isi.dan.gestion.service.TipoHabitacionService;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tarifas contra una base real (H2) y con RabbitMQ mockeado: reglas de alta y baja sobre filas
 * reales, ACTUALIZAR_PRECIO solo después del commit, la tarea diaria y el cron configurable.
 */
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:tarifas_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS tp_dan",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "rabbitmq.exchange=dan.exchange",
      "rabbitmq.routingkey.prefix=dan.habitacion",
      "tarifas.cron-cambio-precio=0 30 1 * * *"
    })
@ActiveProfiles("test")
class TarifasIntegrationTest {

  private static final LocalDate HOY = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"));

  @MockitoBean private RabbitTemplate rabbitTemplate;
  @MockitoBean private ConnectionFactory connectionFactory;
  @MockitoBean private AmqpAdmin amqpAdmin;

  @Autowired private TarifaService tarifaService;
  @Autowired private TipoHabitacionService tipoHabitacionService;
  @Autowired private TarifaPrecioScheduler scheduler;
  @Autowired private ScheduledTaskHolder scheduledTaskHolder;
  @Autowired private TarifaRepository tarifaRepository;
  @Autowired private HabitacionRepository habitacionRepository;
  @Autowired private HotelRepository hotelRepository;
  @Autowired private TipoHabitacionRepository tipoHabitacionRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private ObjectMapper objectMapper;

  private TipoHabitacion tipo;

  @BeforeEach
  void setUp() {
    habitacionRepository.deleteAll();
    tarifaRepository.deleteAll();
    hotelRepository.deleteAll();
    tipoHabitacionRepository.deleteAll();
    tipo = tipoHabitacionRepository.save(new TipoHabitacion(null, "DOBLE", "Doble", 2));
    reset(rabbitTemplate);
  }

  private TarifaRequest comun(double precio) {
    return TarifaRequest.builder()
        .nombre("Tarifa " + precio)
        .idTipoHabitacion(tipo.getId())
        .precioNoche(precio)
        .build();
  }

  private TarifaRequest promo(LocalDate inicio, LocalDate fin, double precio) {
    return TarifaRequest.builder()
        .nombre("Promo")
        .idTipoHabitacion(tipo.getId())
        .precioNoche(precio)
        .fechaInicio(inicio)
        .fechaFin(fin)
        .build();
  }

  private List<Tarifa> enBase() {
    return tarifaRepository.findByTipoHabitacionIdOrderByFechaInicioAsc(tipo.getId());
  }

  private List<HabitacionEvent> mensajesDePrecio(int cantidad) throws Exception {
    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    verify(rabbitTemplate, times(cantidad))
        .convertAndSend(eq("dan.exchange"), eq("dan.habitacion.precio"), payload.capture());
    return payload.getAllValues().stream()
        .map(
            p -> {
              try {
                return objectMapper.readValue((String) p, HabitacionEvent.class);
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            })
        .collect(Collectors.toList());
  }

  @Test
  void flujoCompleto_tarifaNueva_promocionYBajaDeLaVigente() throws Exception {
    // tipo sin tarifas: se crea con inicio hoy y fin null
    TarifaResponse hoyMismo = tarifaService.crear(comun(9000));
    assertEquals(HOY, hoyMismo.fechaInicio());
    assertNull(hoyMismo.fechaFin());
    // una tarifa que empezó antes de hoy (la dejamos como la "tarifa actual" de ahora en más)
    tarifaRepository.deleteAll();
    TarifaResponse primera =
        new TarifaResponse(
            tarifaRepository
                .save(
                    Tarifa.builder()
                        .nombre("Base")
                        .tipoHabitacion(tipo)
                        .fechaInicio(HOY.minusDays(30))
                        .precioNoche(10000.0)
                        .especial(false)
                        .build())
                .getId(),
            "Base",
            tipo.getId(),
            HOY.minusDays(30),
            null,
            10000.0,
            false);
    reset(rabbitTemplate);

    // nueva tarifa sin fechas: la anterior termina ayer
    TarifaResponse segunda = tarifaService.crear(comun(12000));
    assertEquals(
        HOY.minusDays(1), tarifaRepository.findById(primera.id()).orElseThrow().getFechaFin());
    assertEquals(HOY, segunda.fechaInicio());
    assertEquals(
        12000.0, tarifaService.obtenerTarifaVigente(tipo.getId()).orElseThrow().getPrecioNoche());

    // promoción dentro de 10 a 15 días
    LocalDate ini = HOY.plusDays(10);
    LocalDate fin = HOY.plusDays(15);
    TarifaResponse promo = tarifaService.crear(promo(ini, fin, 9000));
    assertTrue(promo.especial());
    List<Tarifa> tarifas = enBase();
    assertEquals(4, tarifas.size());
    assertEquals(ini.minusDays(1), tarifas.get(1).getFechaFin());
    assertEquals(fin.plusDays(1), tarifas.get(3).getFechaInicio());
    assertNull(tarifas.get(3).getFechaFin());
    assertEquals(12000.0, tarifas.get(3).getPrecioNoche());

    // precio según la fecha
    assertEquals(
        12000.0,
        tarifaService
            .obtenerTarifaVigente(tipo.getId(), ini.minusDays(1))
            .orElseThrow()
            .getPrecioNoche());
    assertEquals(
        9000.0,
        tarifaService.obtenerTarifaVigente(tipo.getId(), ini).orElseThrow().getPrecioNoche());
    assertEquals(
        9000.0,
        tarifaService.obtenerTarifaVigente(tipo.getId(), fin).orElseThrow().getPrecioNoche());
    assertEquals(
        12000.0,
        tarifaService
            .obtenerTarifaVigente(tipo.getId(), fin.plusDays(1))
            .orElseThrow()
            .getPrecioNoche());

    // una promo que se pisa con la anterior
    assertThrows(
        IllegalStateException.class,
        () -> tarifaService.crear(promo(ini.plusDays(2), fin.plusDays(2), 8000)));

    // baja de la vigente (la segunda): la primera se extiende y pasa a ser la vigente...
    // pero hay tarifas futuras: la anterior (primera) cubre hasta donde terminaba la borrada
    tarifaService.eliminar(segunda.id());
    List<Tarifa> luego = enBase();
    assertEquals(3, luego.size());
    assertEquals(ini.minusDays(1), luego.get(0).getFechaFin());
    assertEquals(
        10000.0, tarifaService.obtenerTarifaVigente(tipo.getId()).orElseThrow().getPrecioNoche());

    // mensajes: la tarifa nueva (12000) y, al borrarla, la anterior vuelve a ser la vigente
    // (10000);
    // la promo es futura y no cambia el precio de hoy
    List<HabitacionEvent> mensajes = mensajesDePrecio(2);
    assertEquals(
        List.of(12000.0, 10000.0),
        mensajes.stream().map(m -> m.getTarifa().getNuevoPrecio()).toList());
    mensajes.forEach(
        m -> {
          assertEquals(TipoEvento.ACTUALIZAR_PRECIO, m.getTipoEvento());
          assertEquals(tipo.getId(), m.getTarifa().getTipoHabitacionId());
        });
  }

  @Test
  void eliminar_laUnicaTarifa_dejaLaBaseIntacta() {
    TarifaResponse unica = tarifaService.crear(comun(10000));

    assertThrows(IllegalStateException.class, () -> tarifaService.eliminar(unica.id()));
    assertEquals(1, enBase().size());
  }

  @Test
  void actualizarPrecio_seEnviaDespuesDelCommitYNoEnUnRollback() {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              tarifaService.crear(comun(10000));
              status.setRollbackOnly();
            });

    verifyNoInteractions(rabbitTemplate);
    assertEquals(0, enBase().size());
  }

  @Test
  void tareaDiaria_publicaLosTiposCuyaTarifaVigenteCambioHoy() throws Exception {
    LocalDate ayer = HOY.minusDays(1);
    // base hasta ayer a 10000; promo que empieza HOY a 8000; vuelve a 10000 al terminar
    tarifaRepository.save(
        Tarifa.builder()
            .nombre("Base")
            .tipoHabitacion(tipo)
            .fechaInicio(HOY.minusDays(60))
            .fechaFin(ayer)
            .precioNoche(10000.0)
            .especial(false)
            .build());
    tarifaRepository.save(
        Tarifa.builder()
            .nombre("Promo")
            .tipoHabitacion(tipo)
            .fechaInicio(HOY)
            .fechaFin(HOY.plusDays(5))
            .precioNoche(8000.0)
            .especial(true)
            .build());
    tarifaRepository.save(
        Tarifa.builder()
            .nombre("Base")
            .tipoHabitacion(tipo)
            .fechaInicio(HOY.plusDays(6))
            .fechaFin(null)
            .precioNoche(10000.0)
            .especial(false)
            .build());

    scheduler.publicarCambiosDePrecio();

    List<HabitacionEvent> mensajes = mensajesDePrecio(1);
    assertEquals(8000.0, mensajes.get(0).getTarifa().getNuevoPrecio());
    assertEquals(tipo.getId(), mensajes.get(0).getTarifa().getTipoHabitacionId());
  }

  @Test
  void tareaDiaria_sinCambiosHoy_noPublicaNada() {
    tarifaRepository.save(
        Tarifa.builder()
            .nombre("Base")
            .tipoHabitacion(tipo)
            .fechaInicio(HOY.minusDays(60))
            .fechaFin(null)
            .precioNoche(10000.0)
            .especial(false)
            .build());

    scheduler.publicarCambiosDePrecio();

    verifyNoInteractions(rabbitTemplate);
  }

  @Test
  void tareaDiaria_estaProgramadaConElCronDeLasProperties() {
    List<String> crones =
        scheduledTaskHolder.getScheduledTasks().stream()
            .map(ScheduledTask::getTask)
            .filter(CronTask.class::isInstance)
            .map(t -> ((CronTask) t).getExpression())
            .toList();

    assertTrue(crones.contains("0 30 1 * * *"), "crones registrados: " + crones);
  }

  @Test
  void tipoHabitacion_crudConIdGenerado() {
    TipoHabitacionResponse creado =
        tipoHabitacionService.crear(new TipoHabitacionRequest("SUITE", "Suite con living", 4));
    assertNotNull(creado.id());

    TipoHabitacionResponse editado =
        tipoHabitacionService.actualizar(
            creado.id(), new TipoHabitacionRequest("SUITE JR", "Suite chica", 3));
    assertEquals("SUITE JR", tipoHabitacionService.obtener(creado.id()).nombre());
    assertEquals(3, editado.capacidad());

    tipoHabitacionService.eliminar(creado.id());
    assertFalse(tipoHabitacionRepository.existsById(creado.id()));
  }

  @Test
  void tipoHabitacion_conTarifasNoSePuedeEliminar() {
    tarifaService.crear(comun(10000));

    assertThrows(IllegalStateException.class, () -> tipoHabitacionService.eliminar(tipo.getId()));
    assertTrue(tipoHabitacionRepository.existsById(tipo.getId()));
  }
}
