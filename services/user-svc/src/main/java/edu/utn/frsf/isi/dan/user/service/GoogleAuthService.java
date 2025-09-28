package edu.utn.frsf.isi.dan.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthRequest;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.Usuario;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

  @Value("${google.oauth.clientId:}")
  private String googleClientId;

  @Autowired private UsuarioRepository usuarioRepository;

  @PostConstruct
  public void validateConfig() {
    if (googleClientId == null || googleClientId.isBlank()) {
      throw new IllegalStateException(
          "google.oauth.clientId no está configurado en application.properties");
    }
  }

  public GoogleAuthResponse authenticate(GoogleAuthRequest request) {
    try {
      var transport = new NetHttpTransport();
      var jsonFactory = GsonFactory.getDefaultInstance();
      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
              .setAudience(Collections.singletonList(googleClientId))
              .build();

      GoogleIdToken idToken = verifier.verify(request.credential());
      if (idToken == null) {
        throw new SecurityException("ID Token de Google inválido");
      }
      Payload payload = idToken.getPayload();

      String email = payload.getEmail();
      String name = (String) payload.get("name");
      String givenName = (String) payload.get("given_name");
      String familyName = (String) payload.get("family_name");

      Usuario usuario = usuarioRepository.findByEmail(email);
      if (usuario == null) {
        // Por defecto, creamos un Huesped básico con los datos disponibles
        Huesped nuevo = Huesped.builder().email(email).nombre(name).build();
        usuario = usuarioRepository.save(nuevo);
      }

      return new GoogleAuthResponse(
          usuario.getId(), email, usuario.getNombre(), givenName, familyName);
    } catch (Exception e) {
      throw new SecurityException("Error verificando token de Google: " + e.getMessage(), e);
    }
  }
}
