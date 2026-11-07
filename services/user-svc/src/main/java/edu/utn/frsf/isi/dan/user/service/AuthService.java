package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.dto.LoginRequest;
import edu.utn.frsf.isi.dan.user.exception.CredencialesInvalidasException;
import edu.utn.frsf.isi.dan.user.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Login con usuario y contraseña (BCrypt). Devuelve lo mismo que el login de Google. */
@Service
public class AuthService {

  @Autowired private UsuarioRepository usuarioRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  /** Hash BCrypt de una clave cualquiera: se compara igual cuando el usuario no existe. */
  private volatile String hashFicticio;

  public GoogleAuthResponse login(LoginRequest request) {
    Usuario usuario = usuarioRepository.findByUsername(request.username());
    String hash = usuario != null ? usuario.getPassword() : null;

    if (hash == null || hash.isBlank()) {
      // Usuario inexistente o cuenta creada con Google (sin contraseña): se hace igual un matches
      // para que el tiempo de respuesta no delate que el usuario no existe.
      passwordEncoder.matches(request.password(), hashFicticio());
      throw new CredencialesInvalidasException();
    }
    if (!passwordEncoder.matches(request.password(), hash)) {
      throw new CredencialesInvalidasException();
    }

    String nombre = usuario.getNombre();
    String given = null;
    String family = null;
    if (nombre != null && !nombre.isBlank()) {
      String[] partes = nombre.trim().split("\\s+", 2);
      given = partes[0];
      family = partes.length > 1 ? partes[1] : null;
    }
    return new GoogleAuthResponse(usuario.getId(), usuario.getEmail(), nombre, given, family);
  }

  private String hashFicticio() {
    if (hashFicticio == null) {
      hashFicticio = passwordEncoder.encode("no-existe");
    }
    return hashFicticio;
  }
}
