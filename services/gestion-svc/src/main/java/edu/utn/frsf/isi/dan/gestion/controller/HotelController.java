package edu.utn.frsf.isi.dan.gestion.controller;

import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hoteles")
@Tag(name = "Hotel", description = "Operaciones para la gestión de hoteles")
public class HotelController {
    @Autowired
    private HotelService hotelService;

    @Operation(summary = "Registrar un nuevo hotel",
                description = "Permite registrar un nuevo hotel en el sistema.",
                responses = {
                    @ApiResponse(responseCode = "201", description = "Hotel registrado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error al registrar el hotel"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                })
    @PostMapping("/registrar")
    public ResponseEntity<HotelResponse> crearHotel(@Valid @RequestBody HotelRequest hotelRequest) {
        return new ResponseEntity<>(hotelService.crearHotel(hotelRequest), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar un hotel existente",
                description = "Permite actualizar la categoría, el teléfono y el correo de contacto de un hotel existente.",
                responses = {
                    @ApiResponse(responseCode = "200", description = "Hotel actualizado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error al actualizar el hotel"),
                    @ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                })
    @PutMapping("/{id}")
    public ResponseEntity<HotelResponse> actualizarHotel(@PathVariable Integer id, @Valid @RequestBody HotelRequest hotelRequest) {
        return new ResponseEntity<>(hotelService.actualizarHotel(id, hotelRequest), HttpStatus.OK);
    }

    @Operation(summary = "Agregar amenities a un hotel",
                description = "Permite agregar uno o más amenities a un hotel existente.",
                responses = {
                    @ApiResponse(responseCode = "200", description = "Amenities agregados correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error al agregar amenities"),
                    @ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                })
    @PutMapping("/{id}/amenities")
    public ResponseEntity<HotelResponse> agregarAmenities(@PathVariable Integer id, @RequestBody List<AmenityHotelRequest> amenityRequests) {
        return new ResponseEntity<>(hotelService.agregarAmenities(id, amenityRequests), HttpStatus.OK);
    }
}
