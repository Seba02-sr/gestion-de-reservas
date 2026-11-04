package edu.utn.frsf.isi.dan.reservas_svc.controller;

import edu.utn.frsf.isi.dan.reservas_svc.dto.BusquedaHabitacionesRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.service.HabitacionService;
import edu.utn.frsf.isi.dan.reservas_svc.service.ReservasMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Solo lectura: las habitaciones no se crean, modifican ni borran por HTTP. La fuente de verdad es
 * gestion-svc y esta colección se mantiene únicamente con sus mensajes de RabbitMQ.
 */
@RestController
@RequestMapping("/habitaciones")
@Tag(name = "Habitaciones", description = "Réplica local de habitaciones (solo consulta)")
public class HabitacionController {
  @Autowired private HabitacionService habitacionService;
  @Autowired private ReservasMapper mapper;

  @Operation(summary = "Listar todas las habitaciones replicadas")
  @GetMapping
  public List<HabitacionResponse> getAll() {
    return habitacionService.findAll().stream().map(mapper::toResponse).toList();
  }

  @Operation(
      summary = "Buscar habitaciones disponibles",
      description =
          "Filtros opcionales y combinables (AND): fechas (checkIn+checkOut), cantidadHuespedes"
              + " (capacidad >= n), precioMin/precioMax, estrellas (categoría del hotel >= n),"
              + " amenities (contiene todas) y cercanía (lat+lng+radioKm). Una habitación está"
              + " ocupada si tiene una reserva no cancelada con checkIn < checkOut pedido y"
              + " (checkOut nulo o checkOut > checkIn pedido).")
  @ApiResponse(responseCode = "400", description = "Filtros inválidos")
  @GetMapping("/disponibles")
  public List<HabitacionResponse> disponibles(
      @Valid @ParameterObject BusquedaHabitacionesRequest filtros) {
    return habitacionService.buscarDisponibles(filtros).stream().map(mapper::toResponse).toList();
  }

  @Operation(summary = "Obtener una habitación por su id de gestión")
  @ApiResponse(responseCode = "404", description = "No existe la habitación")
  @GetMapping("/{habitacionId}")
  public HabitacionResponse getById(@PathVariable Long habitacionId) {
    return habitacionService
        .findByHabitacionId(habitacionId)
        .map(mapper::toResponse)
        .orElseThrow(
            () -> new RecursoNoEncontradoException("No existe la habitación " + habitacionId));
  }
}
