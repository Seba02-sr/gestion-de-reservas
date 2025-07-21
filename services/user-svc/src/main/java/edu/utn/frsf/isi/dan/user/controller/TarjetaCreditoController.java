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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/tarjetas")
public class TarjetaCreditoController {

    @Autowired
    private TarjetaCreditoService tarjetaCreditoService;

    // GET /tarjetas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<TarjetaCreditoResponse> getTarjetaPorId(@PathVariable("id") @Positive Long id){
        return ResponseEntity.ok(tarjetaCreditoService.getTarjetaById(id));
    }
    

    // GET /huespedes/{id}/tarjetas
    @GetMapping("/huespedes/{huespedId}/tarjetas")
    public ResponseEntity<List<TarjetaCreditoResponse>> getTarjetasPorHuesped(@PathVariable("huespedId") @Positive Long huespedId){
        List<TarjetaCreditoResponse> tarjetas = tarjetaCreditoService.tarjetaCreditoRepository
        .findByHuespedId(huespedId)
        .stream()
        .map(tarjetaCreditoService.tarjetaCreditoMapper::toResponse)
        .toList();

        return ResponseEntity.ok(tarjetas);
    }

    // POST /huespedes/{huespedId}/tarjetas
    // @PostMapping("/huespedes/{huespedId}/tarjetas")
    // public ResponseEntity<TarjetaCreditoResponse> crearTarjetaParaHuesped( @PathVariable("huespedId") @Positive Long huespedId, @RequestBody TarjetaCreditoRequest tarjetaCreditoRequest){
    //     TarjetaCreditoResponse nuevaTarjeta = tarjetaCreditoService.crearTarjetaParaHuesped(huespedId, tarjetaCreditoRequest);
    //     return new ResponseEntity<>(nuevaTarjeta, HttpStatus.CREATED);
    // }
    // DELETE /tarejtas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarTarjeta(@PathVariable("id") @Positive Long id){
        tarjetaCreditoService.deleteTarjetaCredito(id);
        return ResponseEntity.noContent().build();
    }

    // PUT /tarjetas/{id}/set-principal
    @PutMapping("/{id}/set-principal") 
    public ResponseEntity<TarjetaCreditoResponse> setTarjetaPrincipal(@PathVariable("id") @Positive Long id){
        TarjetaCreditoResponse tarjetaPrincipal = tarjetaCreditoService.setTarjetaPrincipal(id);
        return ResponseEntity.ok(tarjetaPrincipal);
    }

    // PUT /{id}
    @PutMapping("/{id}")
    public ResponseEntity<TarjetaCreditoResponse> updateTarjetaCredito(@PathVariable("id") @Positive Long id, @Valid @RequestBody TarjetaCreditoRequest tarjetaCreditoRequest){
        TarjetaCreditoResponse tarjetaCreditoActualizada = tarjetaCreditoService.updateTarjetaCredito(id, tarjetaCreditoRequest);
        return ResponseEntity.ok(tarjetaCreditoActualizada);
    }


}
