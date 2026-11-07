package edu.utn.frsf.isi.dan.gestion.controller;

import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaVigenteResponse;
import edu.utn.frsf.isi.dan.gestion.service.HabitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gestión de habitaciones. Cada alta, modificación y baja se notifica a reservas-svc por RabbitMQ.
 *
 * <ul>
 *   <li>POST /habitaciones, PUT /habitaciones/{id}, DELETE /habitaciones/{id}
 *   <li>GET /habitaciones/{id}
 *   <li>GET /habitaciones?cantidadHuespedes&tipoHabitacionId&precioMin&precioMax (filtros
 *       opcionales combinables)
 *   <li>GET /habitaciones/{id}/tarifa-vigente
 *   <li>GET /habitaciones/{id}/disponible
 * </ul>
 */
@RestController
@RequestMapping("/habitaciones")
@Tag(name = "Habitación", description = "Operaciones para la gestión de habitaciones")
public class HabitacionController {

  @Autowired private HabitacionService habitacionService;

  @Operation(summary = "Crear una habitación")
  @PostMapping
  public ResponseEntity<HabitacionResponse> crear(@Valid @RequestBody HabitacionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(habitacionService.crear(request));
  }

  @Operation(summary = "Obtener una habitación por id")
  @GetMapping("/{id}")
  public ResponseEntity<HabitacionResponse> obtener(@PathVariable Integer id) {
    return ResponseEntity.ok(habitacionService.obtener(id));
  }

  @Operation(
      summary = "Consultar habitaciones",
      description =
          "Filtros opcionales y combinables: capacidad del tipo >= cantidadHuespedes, tipo de"
              + " habitación y rango de precio sobre la tarifa vigente.")
  @GetMapping
  public ResponseEntity<List<HabitacionResponse>> buscar(
      @RequestParam(required = false) Integer cantidadHuespedes,
      @RequestParam(required = false) Integer tipoHabitacionId,
      @RequestParam(required = false) Double precioMin,
      @RequestParam(required = false) Double precioMax) {
    return ResponseEntity.ok(
        habitacionService.buscar(cantidadHuespedes, tipoHabitacionId, precioMin, precioMax));
  }

  @Operation(summary = "Actualizar una habitación (el hotel no se puede cambiar)")
  @PutMapping("/{id}")
  public ResponseEntity<HabitacionResponse> actualizar(
      @PathVariable Integer id, @Valid @RequestBody HabitacionRequest request) {
    return ResponseEntity.ok(habitacionService.actualizar(id, request));
  }

  @Operation(summary = "Eliminar una habitación")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    habitacionService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Tarifa vigente de la habitación (según su tipo)")
  @GetMapping("/{id}/tarifa-vigente")
  public ResponseEntity<TarifaVigenteResponse> tarifaVigente(@PathVariable Integer id) {
    return ResponseEntity.ok(habitacionService.obtenerTarifaVigente(id));
  }

  @Operation(summary = "Indica si la habitación está disponible")
  @GetMapping("/{id}/disponible")
  public ResponseEntity<Boolean> disponible(@PathVariable Integer id) {
    return ResponseEntity.ok(habitacionService.estaDisponible(id));
  }
}
