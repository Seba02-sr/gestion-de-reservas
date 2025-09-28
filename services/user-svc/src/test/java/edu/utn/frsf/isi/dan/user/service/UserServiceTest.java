package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.mapper.PropietarioMapper;
import edu.utn.frsf.isi.dan.user.model.CuentaBancaria;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.Propietario;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import edu.utn.frsf.isi.dan.user.model.Usuario;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class UserServiceTest {

  @Mock private UsuarioRepository usuarioRepository;
  @Mock private HuespedMapper huespedMapper;
  @Mock private PropietarioMapper propietarioMapper;

  @InjectMocks private UserService userService;

  private HuespedRequest huespedRequest;
  private PropietarioRequest propietarioRequest;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    huespedRequest =
        HuespedRequest.builder()
            .nombre("Juan")
            .email("juan@example.com")
            .telefono("123")
            .dni("1111")
            .username("juan")
            .password("secret")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetasCredito(
                List.of(
                    TarjetaCreditoRequest.builder()
                        .numero("4111111111111111")
                        .nombreTitular("Juan")
                        .fechaVencimiento("12/25")
                        .cvc("123")
                        .esPrincipal(true)
                        .idBanco(1)
                        .build()))
            .build();
    propietarioRequest =
        PropietarioRequest.builder()
            .nombre("Prop")
            .email("p@example.com")
            .telefono("321")
            .dni("2222")
            .username("prop")
            .password("secret")
            .idHotel(1L)
            .cuentaBancaria(
                edu.utn.frsf.isi.dan.user.dto.CuentaRequest.builder()
                    .numeroCuenta("123")
                    .idBanco(1)
                    .build())
            .build();
  }

  @Test
  void crearUsuarioHuesped_DeberiaGuardarConRelaciones() {
    TarjetaCredito tarjeta = TarjetaCredito.builder().id(1).esPrincipal(true).build();
    Huesped entidad =
        Huesped.builder().tarjetaCredito(new java.util.ArrayList<>(List.of(tarjeta))).build();
    when(huespedMapper.toEntity(huespedRequest)).thenReturn(entidad);
    when(usuarioRepository.save(entidad)).thenAnswer(inv -> inv.getArgument(0));

    Huesped resultado = userService.crearUsuarioHuesped(huespedRequest);
    assertNotNull(resultado);
    assertEquals(resultado, resultado.getTarjetaCredito().get(0).getHuesped());
  }

  @Test
  void crearUsuarioHuesped_DeberiaFallar_CuandoRequestInvalido() {
    assertThrows(IllegalArgumentException.class, () -> userService.crearUsuarioHuesped(null));
    HuespedRequest sinTarjetas =
        HuespedRequest.builder()
            .nombre("x")
            .email("a@b.com")
            .telefono("1")
            .dni("2")
            .username("u")
            .password("p")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetasCredito(List.of())
            .build();
    assertThrows(
        IllegalArgumentException.class, () -> userService.crearUsuarioHuesped(sinTarjetas));
  }

  @Test
  void crearUsuarioPropietario_DeberiaGuardarConRelaciones() {
    Propietario entidad = Propietario.builder().cuentaBancaria(new CuentaBancaria()).build();
    when(propietarioMapper.toEntity(propietarioRequest)).thenReturn(entidad);
    when(usuarioRepository.save(entidad)).thenAnswer(inv -> inv.getArgument(0));

    Propietario resultado = userService.crearUsuarioPropietario(propietarioRequest);
    assertNotNull(resultado);
    assertEquals(resultado, resultado.getCuentaBancaria().getPropietario());
  }

  @Test
  void crearUsuarioPropietario_DeberiaFallar_CuandoRequestInvalido() {
    assertThrows(IllegalArgumentException.class, () -> userService.crearUsuarioPropietario(null));
    PropietarioRequest inval =
        PropietarioRequest.builder()
            .nombre("p")
            .email("p@e.com")
            .telefono("1")
            .dni("2")
            .username("u")
            .password("p")
            .idHotel(1L)
            .cuentaBancaria(null)
            .build();
    assertThrows(IllegalArgumentException.class, () -> userService.crearUsuarioPropietario(inval));
  }

  @Test
  void busquedas_DeberianDelegarEnRepositorioPorNombreYDni() {
    PageRequest pr = PageRequest.of(0, 10);
    Page<Usuario> page = new PageImpl<>(List.of());
    when(usuarioRepository.findByNombreContainingIgnoreCase("juan", pr)).thenReturn(page);
    when(usuarioRepository.findByDniContaining("11", pr)).thenReturn(page);
    when(usuarioRepository.findByDni("11")).thenReturn(null);

    assertSame(page, userService.buscarPorNombre("juan", pr));
    assertSame(page, userService.buscarPorDni("11", pr));
    assertNull(userService.buscarPorDniExacto("11"));
  }
}
