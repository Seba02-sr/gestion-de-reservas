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

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hoteles")
@Tag(name = "Hotel", description = "Operaciones para la gestión de hoteles")
public class HotelController {
  @Autowired private HotelService hotelService;

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

    @Operation(summary = "Eliminar un amenity de un hotel",
              description = "Permite eliminar un amenity de un hotel existente.",
              responses = {
                @ApiResponse(responseCode = "200", description = "Amenity eliminado correctamente"),
                @ApiResponse(responseCode = "400", description = "Error al eliminar el amenity"),
                @ApiResponse(responseCode = "404", description = "Hotel o amenity no encontrado"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
              })
    @DeleteMapping("/{id}/amenities/{amenityId}")
    public ResponseEntity<Void> eliminarAmenity(@PathVariable Integer id, @PathVariable Long amenityId) {
      hotelService.eliminarAmenity(id, amenityId);
      return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Cerrar un hotel",
              description = "Permite marcar un hotel como cerrado.",
              responses = {
                @ApiResponse(responseCode = "200", description = "Hotel cerrado correctamente"),
                @ApiResponse(responseCode = "400", description = "Error al cerrar el hotel"),
                @ApiResponse(responseCode = "404", description = "Hotel no encontrado"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
              })
    @PutMapping("/{id}/cerrar")
    public ResponseEntity<HotelResponse> cerrarHotel(@PathVariable Integer id) {
      return new ResponseEntity<>(hotelService.cerrarHotel(id), HttpStatus.OK);
    }

    @Operation(summary = "Consultar hoteles con filtros",
              description = "Permite consultar hoteles aplicando filtros opcionales como nombre, CUIT, domicilio, categoría y amenities.",
              responses = {
                @ApiResponse(responseCode = "200", description = "Consulta realizada correctamente"),
                @ApiResponse(responseCode = "400", description = "Error en los parámetros de consulta"),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor")
              })
  @GetMapping("/consultar")
  public ResponseEntity<List<HotelResponse>> consultarHoteles(
      @RequestParam(required = false) String nombre,
      @RequestParam(required = false) String cuit,
      @RequestParam(required = false) String domicilio,
      @RequestParam(required = false) Integer categoria,
      @RequestParam(required = false) List<Amenity> amenities) {
    return new ResponseEntity<>(hotelService.consultarHoteles(nombre, cuit, domicilio, categoria, amenities), HttpStatus.OK);
  }

  @Operation(summary = "Listar todos los hoteles",
            description = "Permite listar todos los hoteles sin aplicar filtros.",
            responses = {
              @ApiResponse(responseCode = "200", description = "Hoteles listados correctamente"),
              @ApiResponse(responseCode = "500", description = "Error interno del servidor")
            })
  @GetMapping
  public ResponseEntity<List<HotelResponse>> listarHoteles() {
    return new ResponseEntity<>(hotelService.listarHoteles(), HttpStatus.OK);
  }

}
