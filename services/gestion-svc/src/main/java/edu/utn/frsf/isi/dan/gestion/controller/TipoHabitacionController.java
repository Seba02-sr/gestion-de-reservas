package edu.utn.frsf.isi.dan.gestion.controller;

import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.service.TipoHabitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tipos-habitacion")
@Tag(name = "Tipo de habitación", description = "ABM de tipos de habitación")
public class TipoHabitacionController {

  @Autowired private TipoHabitacionService tipoHabitacionService;

  @Operation(summary = "Crear un tipo de habitación")
  @PostMapping
  public ResponseEntity<TipoHabitacionResponse> crear(
      @Valid @RequestBody TipoHabitacionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tipoHabitacionService.crear(request));
  }

  @Operation(summary = "Obtener un tipo de habitación por id")
  @GetMapping("/{id}")
  public ResponseEntity<TipoHabitacionResponse> obtener(@PathVariable Integer id) {
    return ResponseEntity.ok(tipoHabitacionService.obtener(id));
  }

  @Operation(summary = "Listar los tipos de habitación")
  @GetMapping
  public ResponseEntity<List<TipoHabitacionResponse>> listar() {
    return ResponseEntity.ok(tipoHabitacionService.listar());
  }

  @Operation(summary = "Actualizar un tipo de habitación")
  @PutMapping("/{id}")
  public ResponseEntity<TipoHabitacionResponse> actualizar(
      @PathVariable Integer id, @Valid @RequestBody TipoHabitacionRequest request) {
    return ResponseEntity.ok(tipoHabitacionService.actualizar(id, request));
  }

  @Operation(
      summary = "Eliminar un tipo de habitación",
      description = "409 si el tipo tiene habitaciones o tarifas.")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    tipoHabitacionService.eliminar(id);
    return ResponseEntity.noContent().build();
  }
}
