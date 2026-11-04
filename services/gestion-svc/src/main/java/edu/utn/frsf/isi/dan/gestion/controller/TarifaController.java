package edu.utn.frsf.isi.dan.gestion.controller;

import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Las tarifas solo se crean (POST) o se eliminan (DELETE): no hay modificación. */
@RestController
@RequestMapping("/tarifas")
@Tag(name = "Tarifa", description = "Alta y baja de tarifas por tipo de habitación")
public class TarifaController {

  @Autowired private TarifaService tarifaService;

  @Operation(
      summary = "Crear una tarifa",
      description =
          "Sin fechas: tarifa común, rige desde hoy y reemplaza a la actual (que termina ayer). "
              + "Con fechaInicio y fechaFin: tarifa especial/promocional; la actual termina el día "
              + "anterior al inicio y, al terminar la promo, vuelve el precio anterior.")
  @PostMapping
  public ResponseEntity<TarifaResponse> crear(@Valid @RequestBody TarifaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tarifaService.crear(request));
  }

  @Operation(
      summary = "Eliminar una tarifa",
      description =
          "Solo si el tipo de habitación tiene otra tarifa (409 si es la única). Si era la vigente, "
              + "la anterior extiende su fecha de fin y pasa a ser la vigente.")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
    tarifaService.eliminar(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Listar tarifas (opcionalmente de un tipo de habitación)")
  @GetMapping
  public ResponseEntity<List<TarifaResponse>> listar(
      @RequestParam(required = false) Integer idTipoHabitacion) {
    return ResponseEntity.ok(tarifaService.listar(idTipoHabitacion));
  }

  @Operation(
      summary = "Tarifa vigente de un tipo de habitación",
      description = "Sin 'fecha' devuelve la vigente hoy. 404 si no hay tarifa vigente ese día.")
  @GetMapping("/vigente")
  public ResponseEntity<TarifaResponse> vigente(
      @RequestParam Integer idTipoHabitacion,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fecha) {
    return ResponseEntity.ok(tarifaService.vigente(idTipoHabitacion, fecha));
  }

  @Operation(summary = "Obtener una tarifa por id")
  @GetMapping("/{id}")
  public ResponseEntity<TarifaResponse> obtener(@PathVariable Integer id) {
    return ResponseEntity.ok(tarifaService.obtener(id));
  }
}
