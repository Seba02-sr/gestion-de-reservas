package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.service.PropietarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/propietarios")
@Tag(name = "Propietario Controller", description = "Operaciones para gestionar propietarios")
public class PropietarioController {

  @Autowired private PropietarioService propietarioService;

  @Operation(summary = "Listar propietarios activos")
  @GetMapping
  public ResponseEntity<List<PropietarioResponse>> getAllPropietarios() {
    return ResponseEntity.ok(propietarioService.getAllPropietarios());
  }

  @Operation(summary = "Obtener propietario por ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Propietario encontrado"),
    @ApiResponse(responseCode = "404", description = "Propietario no encontrado")
  })
  @GetMapping("/{id}")
  public ResponseEntity<PropietarioResponse> getPropietarioById(
      @PathVariable @Positive Long id) {
    return ResponseEntity.ok(propietarioService.getPropietarioById(id));
  }

  @Operation(summary = "Actualizar datos de un propietario")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Propietario actualizado")})
  @PutMapping("/{id}")
  public ResponseEntity<PropietarioResponse> actualizarPropietario(
      @PathVariable @Positive Long id, @RequestBody @Valid PropietarioRequest request) {
    return ResponseEntity.ok(propietarioService.actualizarPropietario(id, request));
  }

  @Operation(summary = "Eliminar propietario (no permitido en etapa 1)")
  @ApiResponses({@ApiResponse(responseCode = "405", description = "Operación no permitida")})
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPropietario(@PathVariable @Positive Long id) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
  }

  @Operation(summary = "Buscar propietarios por nombre")
  @GetMapping("/buscar")
  public ResponseEntity<List<PropietarioResponse>> buscarPorNombre(
      @RequestParam @NotBlank String nombre) {
    return ResponseEntity.ok(propietarioService.buscarPorNombre(nombre));
  }

  @Operation(summary = "Buscar propietarios por DNI (parcial)")
  @GetMapping("/dni")
  public ResponseEntity<List<PropietarioResponse>> buscarPorDniParcial(
      @RequestParam @NotBlank String dni) {
    return ResponseEntity.ok(propietarioService.buscarPorDniParcial(dni));
  }

  @Operation(summary = "Buscar propietario por DNI exacto")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Propietario encontrado"),
    @ApiResponse(responseCode = "404", description = "Propietario no encontrado")
  })
  @GetMapping("/dni/{dni}")
  public ResponseEntity<PropietarioResponse> buscarPorDniExacto(@PathVariable String dni) {
    return ResponseEntity.ok(propietarioService.buscarPorDniExacto(dni));
  }
}
