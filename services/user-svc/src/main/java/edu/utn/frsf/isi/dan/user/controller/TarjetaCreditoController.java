package edu.utn.frsf.isi.dan.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.service.TarjetaCreditoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/tarjetas")
public class TarjetaCreditoController {

    @Autowired
    private TarjetaCreditoService tarjetaCreditoService;

    @Operation(summary = "Obtejer una tarjeta de credito por ID", description = "Devuelve una tarejta de credito por su ID", tags = {
            "Tarjetas" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarjeta de Credito encontrada"),
            @ApiResponse(responseCode = "404", description = "Tarjeta de Credito no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    // GET /tarjetas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TarjetaCreditoResponse> getTarjetaPorId(@PathVariable("id") @Positive Long id) {
        return ResponseEntity.ok(tarjetaCreditoService.getTarjetaById(id));
    }

    @Operation(summary = "Obtener todas las tarjetas de un huesped", description = "Devuelve una lista de todas las tarjetas de credito asociadas a un huesped", tags = {
            "Tarjetas" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de tarjetas encontrada"),
            @ApiResponse(responseCode = "404", description = "Huesped no encontrado o sin tarjetas"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    // GET /huespedes/{id}/tarjetas
    @GetMapping("/huespedes/{huespedId}/tarjetas")
    public ResponseEntity<List<TarjetaCreditoResponse>> getTarjetasPorHuesped(
            @PathVariable("huespedId") @Positive Long huespedId) {
        List<TarjetaCreditoResponse> tarjetas = tarjetaCreditoService.tarjetaCreditoRepository
                .findByHuespedId(huespedId)
                .stream()
                .map(tarjetaCreditoService.tarjetaCreditoMapper::toResponse)
                .toList();

        return ResponseEntity.ok(tarjetas);
    }

    /*
     * @Operation(
     * summary = "Crear una nueva tarjeta para un huesped",
     * description = "Asocia una nueva tarjeta de credito a un huesped por su ID",
     * tags = {"Tarjetas"}
     * )
     * 
     * @ApiResponses(value = {
     * 
     * @ApiResponse(responseCode = "201", description =
     * "Tarjeta creada exitosamente"),
     * 
     * @ApiResponse(responseCode = "400", description =
     * "Datos de tarjeta invalidos"),
     * 
     * @ApiResponse(responseCode = "500", description =
     * "Error interno del servidor")
     * })
     * POST /huespedes/{huespedId}/tarjetas
     * 
     * @PostMapping("/huespedes/{huespedId}/tarjetas")
     * public ResponseEntity<TarjetaCreditoResponse> crearTarjetaParaHuesped(
     * 
     * @PathVariable("huespedId") @Positive Long huespedId, @RequestBody
     * TarjetaCreditoRequest tarjetaCreditoRequest){
     * TarjetaCreditoResponse nuevaTarjeta =
     * tarjetaCreditoService.crearTarjetaParaHuesped(huespedId,
     * tarjetaCreditoRequest);
     * return new ResponseEntity<>(nuevaTarjeta, HttpStatus.CREATED);
     * }
     */
    @Operation(summary = "Eliminar una tarjeta por ID", description = "Elimina una tarjeta de credito, no principal, del sistema por su ID", tags = {
            "Tarjetas" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tarjeta eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar la tarjeta principal"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    // DELETE /tarejtas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTarjeta(@PathVariable("id") @Positive Long id) {
        tarjetaCreditoService.deleteTarjetaCredito(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar una tarjeta como principal", description = "Marca una tarjeta como la principal del huesped y desactiva otras", tags = {
            "Tarjetas" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarjeta marcada como principal"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    // PUT /tarjetas/{id}/set-principal
    @PutMapping("/{id}/set-principal")
    public ResponseEntity<TarjetaCreditoResponse> setTarjetaPrincipal(@PathVariable("id") @Positive Long id) {
        TarjetaCreditoResponse tarjetaPrincipal = tarjetaCreditoService.setTarjetaPrincipal(id);
        return ResponseEntity.ok(tarjetaPrincipal);
    }

    @Operation(summary = "Actualizar una tarjeta por ID", description = "Actualiza los datos de una tarjeta existente", tags = {
            "Tarjetas" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarjeta actualizada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Tarjeta no encontrada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    // PUT /{id}
    @PutMapping("/{id}")
    public ResponseEntity<TarjetaCreditoResponse> updateTarjetaCredito(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody TarjetaCreditoRequest tarjetaCreditoRequest) {
        TarjetaCreditoResponse tarjetaCreditoActualizada = tarjetaCreditoService.updateTarjetaCredito(id,
                tarjetaCreditoRequest);
        return ResponseEntity.ok(tarjetaCreditoActualizada);
    }

}
