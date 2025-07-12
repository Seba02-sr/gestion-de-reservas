package edu.utn.frsf.isi.dan.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.utn.frsf.isi.dan.user.model.Propietario;
import edu.utn.frsf.isi.dan.user.service.PropietarioService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/propietarios")
@Tag(name = "Propietario Controller", description = "Operaciones para gestionar propietarios")
public class PropietarioController {

    @Autowired
    private PropietarioService propietarioService;

    @GetMapping
    public ResponseEntity<List<Propietario>> getAllPropietarios() {
        return ResponseEntity.ok(propietarioService.getAllPropietarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Propietario> getPropietarioById(@PathVariable Long id) {
        return propietarioService.getPropietarioById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Propietario> actualizarPropietario(@PathVariable Long id, @RequestBody Propietario actualizado) {
        return ResponseEntity.ok(propietarioService.actualizarPropietario(id, actualizado));
    }

    // No se permite borrar propietarios, según el enunciado.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPropietario(@PathVariable Long id) {
        return ResponseEntity.status(405).build(); // Método no permitido
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Propietario>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(propietarioService.buscarPorNombre(nombre));
    }

    @GetMapping("/dni")
    public ResponseEntity<List<Propietario>> buscarPorDniParcial(@RequestParam String dni) {
        return ResponseEntity.ok(propietarioService.buscarPorDniParcial(dni));
    }

}
