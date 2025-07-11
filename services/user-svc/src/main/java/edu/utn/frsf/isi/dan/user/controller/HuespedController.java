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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.utn.frsf.isi.dan.user.dto.HuespedRecord;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.service.HuespedService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/huespedes")
@Tag(name = "Huesped Controller", description = "Operaciones para gestionar huéspedes")
public class HuespedController {

    @Autowired
    private HuespedService huespedService;

    @GetMapping
    public ResponseEntity<List<Huesped>> getAllHuespedes() {
        return ResponseEntity.ok(huespedService.getAllHuespedes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Huesped> getHuespedById(@PathVariable Long id) {
        return huespedService.getHuespedById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Huesped> actualizarHuesped(@PathVariable Long id, @RequestBody HuespedRecord huespedRecord) {
        Huesped actualizado = huespedRecord.toHuesped();
        return ResponseEntity.ok(huespedService.actualizarHuesped(id, actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarHuesped(@PathVariable Long id) {
        huespedService.eliminarHuesped(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Huesped>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(huespedService.buscarPorNombre(nombre));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<Huesped> buscarPorDni(@PathVariable String dni) {
        return huespedService.buscarPorDni(dni)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
