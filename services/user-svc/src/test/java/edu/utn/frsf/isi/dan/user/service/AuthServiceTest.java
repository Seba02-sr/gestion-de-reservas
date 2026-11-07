package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.dto.LoginRequest;
import edu.utn.frsf.isi.dan.user.exception.CredencialesInvalidasException;
import edu.utn.frsf.isi.dan.user.exception.GoogleNoConfiguradoException;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  @InjectMocks private AuthService authService;

  private Huesped juan;

  @BeforeEach
  void setUp() {
    juan =
        Huesped.builder()
            .id(5)
            .nombre("Juan Perez")
            .email("juan@x.com")
            .username("juan")
            .password(passwordEncoder.encode("secret"))
            .build();
  }

  @Test
  void loginOk_devuelveIdentidad() {
    when(usuarioRepository.findByUsername("juan")).thenReturn(juan);
    GoogleAuthResponse r = authService.login(new LoginRequest("juan", "secret"));
    assertEquals(5, r.userId());
    assertEquals("juan@x.com", r.email());
    assertEquals("Juan Perez", r.nombre());
    assertEquals("Juan", r.givenName());
    assertEquals("Perez", r.familyName());
  }

  @Test
  void claveIncorrecta_lanzaCredencialesInvalidas() {
    when(usuarioRepository.findByUsername("juan")).thenReturn(juan);
    assertThrows(
        CredencialesInvalidasException.class,
        () -> authService.login(new LoginRequest("juan", "otra")));
  }

  @Test
  void usuarioInexistente_mismoError() {
    when(usuarioRepository.findByUsername("nadie")).thenReturn(null);
    var ex =
        assertThrows(
            CredencialesInvalidasException.class,
            () -> authService.login(new LoginRequest("nadie", "secret")));
    assertEquals("Usuario o contraseña incorrectos", ex.getMessage());
  }

  @Test
  void cuentaDeGoogleSinPassword_noPuedeLoguearseConClave() {
    Huesped google = Huesped.builder().id(9).email("g@x.com").username("g").build();
    when(usuarioRepository.findByUsername("g")).thenReturn(google);
    assertThrows(
        CredencialesInvalidasException.class, () -> authService.login(new LoginRequest("g", "x")));
  }

  @Test
  void googleSinClientId_noExplotaAlCrearseYResponde503() {
    GoogleAuthService google = new GoogleAuthService();
    ReflectionTestUtils.setField(google, "googleClientId", "");
    assertEquals(false, google.estaConfigurado());
    assertThrows(
        GoogleNoConfiguradoException.class,
        () -> google.authenticate(new edu.utn.frsf.isi.dan.user.dto.GoogleAuthRequest("abc")));
  }
}
