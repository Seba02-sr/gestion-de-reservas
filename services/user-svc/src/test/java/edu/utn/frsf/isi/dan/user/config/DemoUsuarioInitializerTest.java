package edu.utn.frsf.isi.dan.user.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoUsuarioInitializerTest {

  private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
  private final BancoRepository bancos = mock(BancoRepository.class);
  private final UserService userService = mock(UserService.class);
  private final DemoUsuarioInitializer initializer =
      new DemoUsuarioInitializer(usuarios, bancos, userService);

  @Test
  void creaElHuespedDemoConUnaTarjetaPrincipal() {
    when(usuarios.findByUsername("demo")).thenReturn(null);
    when(bancos.findByActivoTrue()).thenReturn(List.of(Banco.builder().id(7).nombre("B").build()));

    initializer.run(null);

    ArgumentCaptor<HuespedRequest> captor = ArgumentCaptor.forClass(HuespedRequest.class);
    verify(userService).crearUsuarioHuesped(captor.capture());
    HuespedRequest req = captor.getValue();
    assertEquals("demo", req.username());
    assertEquals("demo1234", req.password());
    assertEquals(1, req.tarjetasCredito().size());
    assertTrue(req.tarjetasCredito().get(0).esPrincipal());
    assertEquals(7, req.tarjetasCredito().get(0).idBanco());
  }

  @Test
  void siElUsuarioYaExisteNoHaceNada_idempotente() {
    when(usuarios.findByUsername("demo")).thenReturn(Huesped.builder().id(1).build());

    initializer.run(null);
    initializer.run(null);

    verify(userService, never()).crearUsuarioHuesped(any());
  }

  @Test
  void sinBancosNoFallaYNoCrea() {
    when(usuarios.findByUsername("demo")).thenReturn(null);
    when(bancos.findByActivoTrue()).thenReturn(List.of());

    initializer.run(null);

    verify(userService, never()).crearUsuarioHuesped(any());
  }

  @Test
  void soloExisteConLaPropiedadDemoDatosHabilitado() {
    ApplicationContextRunner base =
        new ApplicationContextRunner()
            .withInitializer(
                ctx -> {
                  // singletons ya construidos: Spring no les inyecta campos (son mocks)
                  var bf = ctx.getBeanFactory();
                  bf.registerSingleton("usuarios", usuarios);
                  bf.registerSingleton("bancos", bancos);
                  bf.registerSingleton("userService", userService);
                })
            .withConfiguration(UserConfigurations.of(DemoUsuarioInitializer.class));

    base.run(ctx -> assertTrue(ctx.getBeansOfType(DemoUsuarioInitializer.class).isEmpty()));
    base.withPropertyValues("demo.datos.habilitado=false")
        .run(ctx -> assertTrue(ctx.getBeansOfType(DemoUsuarioInitializer.class).isEmpty()));
    base.withPropertyValues("demo.datos.habilitado=true")
        .run(ctx -> assertEquals(1, ctx.getBeansOfType(DemoUsuarioInitializer.class).size()));
  }
}
