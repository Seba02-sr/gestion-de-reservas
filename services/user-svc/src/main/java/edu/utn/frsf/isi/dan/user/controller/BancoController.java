package edu.utn.frsf.isi.dan.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.service.BancoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Banco Controller", description = "Operaciones para la gestión de bancos")
@RestController
@RequestMapping("/bancos")
public class BancoController {

    @Autowired
    private BancoService bancoService;

    @Operation(summary = "Crear un banco", 
                description = "Crea un nuevo banco",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Banco creado exitosamente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error en la solicitud"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
    )
    @PostMapping
    public ResponseEntity<Void> createBanco(@Valid @RequestBody BancoRequest bancoRecord) {
        bancoService.createBanco(bancoRecord);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener todos los bancos", 
                description = "Devuelve una lista de bancos",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de bancos obtenida exitosamente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
                    )
    @GetMapping
    public ResponseEntity<List<BancoResponse>> getAllBancos() {
        List<BancoResponse> bancos = bancoService.getAllBancos();
        return ResponseEntity.ok(bancos);
    }

    @Operation(summary = "Obtener banco por ID", 
                description = "Devuelve un banco por su ID",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Banco encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Banco no encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
                )
    @GetMapping("/{id}")
    public ResponseEntity<BancoResponse> getBancoById(@PathVariable Integer id) {
        BancoResponse banco = bancoService.getBancoById(id);
        return ResponseEntity.ok(banco);
    }

    @Operation(summary = "Actualizar banco", 
                description = "Actualiza los datos de un banco",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Banco actualizado exitosamente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error en la solicitud"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Banco no encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
                )
    @PutMapping("/{id}")
    public ResponseEntity<BancoResponse> updateBanco(@PathVariable Integer id, @Valid @RequestBody BancoRequest bancoRecord) {
        BancoResponse bancoActualizado = bancoService.updateBanco(id, bancoRecord);
        return ResponseEntity.ok(bancoActualizado);
    }

    @Operation(summary = "Eliminar banco", 
                description = "Elimina un banco por su ID",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Banco eliminado exitosamente"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Banco no encontrado"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
                )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBanco(@PathVariable Integer id) {
        bancoService.deleteBanco(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Buscar bancos por nombre", 
                description = "Devuelve bancos cuyo nombre contiene la cadena indicada",
                responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bancos encontrados"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error en la solicitud"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")}
                )
    @GetMapping("/buscar")
    public ResponseEntity<List<BancoResponse>> getBancosByNombre(@RequestParam String nombre) {
        List<BancoResponse> bancos = bancoService.getBancosByNombre(nombre);
        return ResponseEntity.ok(bancos);
    }


}
