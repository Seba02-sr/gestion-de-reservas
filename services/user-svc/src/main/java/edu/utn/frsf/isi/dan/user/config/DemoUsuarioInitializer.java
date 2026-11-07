package edu.utn.frsf.isi.dan.user.config;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea el huésped de demo (usuario {@value #USERNAME}, contraseña {@value #PASSWORD}) con una
 * tarjeta principal, para poder iniciar sesión y reservar sin registrarse.
 *
 * <p>Solo existe con {@code demo.datos.habilitado=true} (variable {@code DEMO_DATOS_HABILITADO}) y
 * es idempotente: si el usuario ya existe no hace nada. Corre después de {@link
 * BancoDataInitializer} (necesita un banco emisor para la tarjeta).
 */
@Component
@Order(1)
@Slf4j
@ConditionalOnProperty(name = "demo.datos.habilitado", havingValue = "true")
public class DemoUsuarioInitializer implements ApplicationRunner {

  static final String USERNAME = "demo";
  static final String PASSWORD = "demo1234";

  private final UsuarioRepository usuarioRepository;
  private final BancoRepository bancoRepository;
  private final UserService userService;

  public DemoUsuarioInitializer(
      UsuarioRepository usuarioRepository,
      BancoRepository bancoRepository,
      UserService userService) {
    this.usuarioRepository = usuarioRepository;
    this.bancoRepository = bancoRepository;
    this.userService = userService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (usuarioRepository.findByUsername(USERNAME) != null) {
      return;
    }
    List<Banco> bancos = bancoRepository.findByActivoTrue();
    if (bancos.isEmpty()) {
      log.warn("Usuario de demo: no hay bancos cargados, se omite la creación de '{}'", USERNAME);
      return;
    }
    userService.crearUsuarioHuesped(
        HuespedRequest.builder()
            .nombre("Usuario Demo")
            .email("demo@reservas-demo.example")
            .telefono("+54 342 400-0000")
            .dni("30111222")
            .username(USERNAME)
            .password(PASSWORD)
            .fechaNacimiento(LocalDate.of(1990, 5, 20))
            .tarjetasCredito(
                List.of(
                    TarjetaCreditoRequest.builder()
                        .numero("5500000000000004")
                        .nombreTitular("USUARIO DEMO")
                        .fechaVencimiento("12/2030")
                        .cvc("123")
                        .esPrincipal(true)
                        .idBanco(bancos.get(0).getId())
                        .build()))
            .build());
    log.info("Usuario de demo creado: {} / {}", USERNAME, PASSWORD);
  }
}
