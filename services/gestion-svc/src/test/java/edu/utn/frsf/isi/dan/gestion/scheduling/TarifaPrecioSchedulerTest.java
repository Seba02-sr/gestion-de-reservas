package edu.utn.frsf.isi.dan.gestion.scheduling;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class TarifaPrecioSchedulerTest {

  @Mock private TarifaService tarifaService;
  @InjectMocks private TarifaPrecioScheduler scheduler;

  @Test
  void alArrancar_hacePorUnaVezElMismoChequeoQueLaTareaDiaria() {
    when(tarifaService.publicarCambiosDePrecioDelDia()).thenReturn(2);

    scheduler.alArrancar();

    verify(tarifaService, times(1)).publicarCambiosDePrecioDelDia();
  }

  @Test
  void alArrancar_unaFallaNoImpideElArranqueDeLaAplicacion() {
    when(tarifaService.publicarCambiosDePrecioDelDia()).thenThrow(new RuntimeException("db caída"));

    assertDoesNotThrow(() -> scheduler.alArrancar());
  }

  @Test
  void alArrancar_escuchaApplicationReadyEvent() throws Exception {
    Method m = TarifaPrecioScheduler.class.getMethod("alArrancar");

    EventListener listener = m.getAnnotation(EventListener.class);

    assertNotNull(listener);
    assertArrayEquals(new Class<?>[] {ApplicationReadyEvent.class}, listener.value());
  }

  @Test
  void laTareaDiariaTieneUnCronConfigurableConDefaultALas0005() throws Exception {
    Scheduled scheduled =
        TarifaPrecioScheduler.class
            .getMethod("publicarCambiosDePrecio")
            .getAnnotation(Scheduled.class);

    assertEquals("${tarifas.cron-cambio-precio:0 5 0 * * *}", scheduled.cron());
  }
}
