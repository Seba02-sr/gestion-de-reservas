package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.GoogleAuthRequest;
import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.service.GoogleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:8080"})
public class AuthController {

  @Autowired private GoogleAuthService googleAuthService;

  @PostMapping("/google")
  public ResponseEntity<GoogleAuthResponse> loginWithGoogle(
      @RequestBody GoogleAuthRequest request) {
    GoogleAuthResponse response = googleAuthService.authenticate(request);
    return ResponseEntity.ok(response);
  }
}

