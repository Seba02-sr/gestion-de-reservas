package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.UsuarioResponse;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.mapper.UsuarioMapper;
import edu.utn.frsf.isi.dan.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "User Controller", description = "Operaciones para la gestión de usuarios")
@RestController
@RequestMapping("/users")
public class UserController {

  @Autowired private UserService userService;
  @Autowired private UsuarioMapper usuarioMapper;
  @Autowired private HuespedMapper huespedMapper;

  @Operation(
      summary = "Crear usuario huesped",
      description = "Crea un nuevo usuario de tipo huesped",
      responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description =
                "Usuario huesped creado: devuelve el HuespedResponse y el header Location"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Error en la solicitud"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor")
      })
  @PostMapping("/huesped")
  public ResponseEntity<HuespedResponse> crearUsuarioHuesped(
      @RequestBody @Valid HuespedRequest huespedRecord) {
    var creado = userService.crearUsuarioHuesped(huespedRecord);
    // Location relativo al servicio: por el gateway el recurso queda en /users/huespedes/{id}
    var location = UriComponentsBuilder.fromPath("/huespedes/{id}").buildAndExpand(creado.getId());
    return ResponseEntity.created(location.toUri()).body(huespedMapper.toResponse(creado));
  }

  @Operation(
      summary = "Crear usuario propietario",
      description = "Crea un nuevo usuario de tipo propietario")
  @PostMapping("/propietario")
  public ResponseEntity<Void> crearUsuarioPropietario(
      @RequestBody @Valid PropietarioRequest propietarioRecord) {
    userService.crearUsuarioPropietario(propietarioRecord);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping
  public Page<UsuarioResponse> buscarUsuariosPorNombre(
      @RequestParam(required = false) String nombre, Pageable pageable) {
    var page =
        (nombre == null || nombre.isEmpty())
            ? userService.buscarPorNombre("", pageable)
            : userService.buscarPorNombre(nombre, pageable);
    return page.map(usuarioMapper::toResponse);
  }

  @GetMapping("/dni/{dni}")
  public ResponseEntity<UsuarioResponse> buscarUsuarioPorDni(@PathVariable String dni) {
    var usuario = userService.buscarPorDniExacto(dni);
    if (usuario == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
  }

  @GetMapping("/buscar-dni")
  public Page<UsuarioResponse> buscarUsuariosPorDni(@RequestParam String dni, Pageable pageable) {
    return userService.buscarPorDni(dni, pageable).map(usuarioMapper::toResponse);
  }
}
