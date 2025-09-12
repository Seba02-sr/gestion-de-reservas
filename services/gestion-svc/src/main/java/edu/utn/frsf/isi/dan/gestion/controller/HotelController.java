package edu.utn.frsf.isi.dan.gestion.controller;

import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
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

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getById(@PathVariable Integer id) {
        return hotelService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Hotel> getAll() {
        return hotelService.findAll();
    }

    /*
    @PutMapping("/{id}")
    public ResponseEntity<Hotel> update(@PathVariable Integer id, @RequestBody Hotel hotel) {
        if (!hotelService.findById(id).isPresent()) return ResponseEntity.notFound().build();
        hotel.setId(id);
        return ResponseEntity.ok(hotelService.save(hotel));
    }*/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!hotelService.findById(id).isPresent()) return ResponseEntity.notFound().build();
        hotelService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
