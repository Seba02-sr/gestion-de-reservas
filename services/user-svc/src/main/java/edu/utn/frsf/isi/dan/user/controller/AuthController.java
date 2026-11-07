package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.GoogleAuthRequest;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.dto.LoginRequest;
import edu.utn.frsf.isi.dan.user.service.AuthService;
import edu.utn.frsf.isi.dan.user.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
// CORS: se configura solo en el API Gateway (dan-spring-gateway), no en los servicios.
public class AuthController {

  @Autowired private GoogleAuthService googleAuthService;
  @Autowired private AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<GoogleAuthResponse> login(@RequestBody @Valid LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/google")
  public ResponseEntity<GoogleAuthResponse> loginWithGoogle(
      @RequestBody GoogleAuthRequest request) {
    GoogleAuthResponse response = googleAuthService.authenticate(request);
    return ResponseEntity.ok(response);
  }
}
