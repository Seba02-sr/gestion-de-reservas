package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.service.HuespedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/huespedes")
@Tag(name = "Huesped Controller", description = "Operaciones para gestionar huéspedes")
public class HuespedController {

  @Autowired private HuespedService huespedService;

  @Operation(summary = "Listar huéspedes activos")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Listado obtenido")})
  @GetMapping
  public ResponseEntity<List<HuespedResponse>> getAllHuespedes() {
    return ResponseEntity.ok(huespedService.getAllHuespedes());
  }

  @Operation(summary = "Obtener huésped por ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Huésped encontrado"),
    @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
  })
  @GetMapping("/{id}")
  public ResponseEntity<HuespedResponse> getHuespedById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(huespedService.getHuespedById(id));
  }

  @Operation(summary = "Actualizar datos de un huésped")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Huésped actualizado"),
    @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
  })
  @PutMapping("/{id}")
  public ResponseEntity<HuespedResponse> actualizarHuesped(
      @PathVariable @Positive Long id, @RequestBody @Valid HuespedRequest huespedRequest) {
    return ResponseEntity.ok(huespedService.actualizarHuesped(id, huespedRequest));
  }

  @Operation(summary = "Eliminar (soft-delete) un huésped")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Huésped eliminado")})
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarHuesped(@PathVariable @Positive Long id) {
    huespedService.eliminarHuesped(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Buscar huéspedes por nombre (activos)")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Búsqueda realizada")})
  @GetMapping("/buscar")
  public ResponseEntity<List<HuespedResponse>> buscarPorNombre(
      @RequestParam @NotBlank String nombre) {
    return ResponseEntity.ok(huespedService.buscarPorNombre(nombre));
  }

  @Operation(summary = "Buscar huésped por DNI exacto (activo)")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Huésped encontrado"),
    @ApiResponse(responseCode = "404", description = "Huésped no encontrado")
  })
  @GetMapping("/dni/{dni}")
  public ResponseEntity<HuespedResponse> buscarPorDni(@PathVariable @NotBlank String dni) {
    return ResponseEntity.ok(huespedService.buscarPorDni(dni));
  }

  // Variante con query param por compatibilidad con tests/consumidores
  @Operation(hidden = true)
  @GetMapping("/dni")
  public ResponseEntity<HuespedResponse> buscarPorDniQuery(@RequestParam("dni") String dni) {
    return ResponseEntity.ok(huespedService.buscarPorDni(dni));
  }
}
