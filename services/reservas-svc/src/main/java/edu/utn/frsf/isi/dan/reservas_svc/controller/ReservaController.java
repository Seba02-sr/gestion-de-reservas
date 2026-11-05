package edu.utn.frsf.isi.dan.reservas_svc.controller;

import edu.utn.frsf.isi.dan.reservas_svc.dto.BloqueoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.EstadoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.PagoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReviewRequest;
import edu.utn.frsf.isi.dan.reservas_svc.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas", description = "Alta, pagos, estados y reviews de reservas")
public class ReservaController {
  @Autowired private ReservaService reservaService;

  @Operation(
      summary = "Crear una reserva",
      description =
          "habitacionId es el id de la habitación en gestion-svc. Queda RESERVADA. 400 si las fechas"
              + " no son válidas, 404 si la habitación no existe, 409 si no tiene tarifa vigente o"
              + " está ocupada en esas fechas.")
  @ApiResponse(responseCode = "201", description = "Reserva creada")
  @PostMapping
  public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crear(request));
  }

  @Operation(
      summary = "Bloquear una habitación (dueño)",
      description =
          "Crea una reserva BLOQUEADA sin huésped ni precio. Para desbloquear: PATCH"
              + " /reservas/{id}/estado con CANCELADA.")
  @PostMapping("/bloqueos")
  public ResponseEntity<ReservaResponse> bloquear(@Valid @RequestBody BloqueoRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearBloqueo(request));
  }

  @Operation(summary = "Listar reservas (opcionalmente de un huésped)")
  @GetMapping
  public List<ReservaResponse> listar(@RequestParam(required = false) Integer idHuesped) {
    return reservaService.listarPorHuesped(idHuesped);
  }

  @Operation(summary = "Obtener una reserva por id")
  @ApiResponse(responseCode = "404", description = "No existe la reserva")
  @GetMapping("/{id}")
  public ReservaResponse obtener(@PathVariable String id) {
    return reservaService.obtener(id);
  }

  @Operation(
      summary = "Registrar un pago",
      description =
          "Con el primer pago la reserva pasa a CONFIRMADA. 409 si la reserva no admite pagos o el"
              + " monto excede el saldo.")
  @PostMapping("/{id}/pagos")
  public ResponseEntity<ReservaResponse> pagar(
      @PathVariable String id, @Valid @RequestBody PagoRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.agregarPago(id, request));
  }

  @Operation(
      summary = "Cambiar el estado de una reserva",
      description =
          "CANCELADA: solo RESERVADA/BLOQUEADA sin pagos (libera las fechas). FINALIZADA: desde"
              + " RESERVADA, CONFIRMADA o ADEUDADA; con review del dueño y pago completo queda"
              + " FINALIZADA, si no queda ADEUDADA. Cualquier otro estado devuelve 409.")
  @PatchMapping("/{id}/estado")
  public ReservaResponse cambiarEstado(
      @PathVariable String id, @Valid @RequestBody EstadoRequest request) {
    return reservaService.cambiarEstado(id, request.estado());
  }

  @Operation(
      summary = "Calificación del huésped",
      description = "Solo después del check-out (rating 1 a 5).")
  @PutMapping("/{id}/review-cliente")
  public ReservaResponse reviewCliente(
      @PathVariable String id, @Valid @RequestBody ReviewRequest request) {
    return reservaService.reviewCliente(id, request);
  }

  @Operation(
      summary = "Review del dueño",
      description = "Requisito para finalizar la reserva (hostReview).")
  @PutMapping("/{id}/review-dueno")
  public ReservaResponse reviewDueno(
      @PathVariable String id, @Valid @RequestBody ReviewRequest request) {
    return reservaService.reviewDueno(id, request);
  }
}
