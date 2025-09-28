package edu.utn.frsf.isi.dan.user.controller;

import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.service.BancoService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Banco Controller", description = "Operaciones para la gestión de bancos")
@RestController
@RequestMapping("/bancos")
public class BancoController {

  @Autowired private BancoService bancoService;

  @Operation(summary = "Crear un banco", description = "Crea un nuevo banco")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "201", description = "Banco creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la solicitud"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @PostMapping
  public ResponseEntity<BancoResponse> createBanco(@Valid @RequestBody BancoRequest bancoRecord) {
    BancoResponse banco = bancoService.createBanco(bancoRecord);
    return ResponseEntity.status(HttpStatus.CREATED).body(banco);
  }

  @Operation(summary = "Obtener todos los bancos", description = "Devuelve una lista de bancos")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lista de bancos obtenida exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @GetMapping
  public ResponseEntity<List<BancoResponse>> getAllBancos() {
    List<BancoResponse> bancos = bancoService.getAllBancos();
    return ResponseEntity.ok(bancos);
  }

  @Operation(summary = "Obtener banco por ID", description = "Devuelve un banco por su ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Banco encontrado"),
        @ApiResponse(responseCode = "400", description = "ID inválido"),
        @ApiResponse(responseCode = "404", description = "Banco no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @GetMapping("/{id}")
  public ResponseEntity<BancoResponse> getBancoById(@PathVariable @Positive Integer id) {
    BancoResponse banco = bancoService.getBancoById(id);
    return ResponseEntity.ok(banco);
  }

  @Operation(summary = "Actualizar banco", description = "Actualiza los datos de un banco")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Banco actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error en la solicitud"),
        @ApiResponse(responseCode = "404", description = "Banco no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @PutMapping("/{id}")
  public ResponseEntity<BancoResponse> updateBanco(
      @PathVariable @Positive Integer id, @Valid @RequestBody BancoRequest bancoRecord) {
    BancoResponse bancoActualizado = bancoService.updateBanco(id, bancoRecord);
    return ResponseEntity.ok(bancoActualizado);
  }

  @Operation(summary = "Eliminar banco", description = "Elimina un banco por su ID")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Banco eliminado exitosamente"),
        @ApiResponse(responseCode = "400", description = "ID inválido"),
        @ApiResponse(responseCode = "404", description = "Banco no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @DeleteMapping("/{id}")
  public ResponseEntity<BancoResponse> deleteBanco(@PathVariable @Positive Integer id) {
    BancoResponse bancoEliminado = bancoService.deleteBanco(id);
    return ResponseEntity.ok(bancoEliminado);
  }

  @Operation(
      summary = "Buscar bancos por nombre",
      description = "Devuelve bancos cuyo nombre contiene la cadena indicada")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Bancos encontrados"),
        @ApiResponse(responseCode = "400", description = "Error en la solicitud"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
      })
  @GetMapping("/buscar")
  public ResponseEntity<List<BancoResponse>> getBancosByNombre(
      @RequestParam @NotBlank String nombre) {
    List<BancoResponse> bancos = bancoService.getBancosByNombre(nombre);
    return ResponseEntity.ok(bancos);
  }
}
