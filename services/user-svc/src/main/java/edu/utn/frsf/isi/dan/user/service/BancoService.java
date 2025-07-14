package edu.utn.frsf.isi.dan.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.persistence.EntityNotFoundException;

@Service
public class BancoService {

    @Autowired
    private BancoRepository bancoRepository;

    public List<BancoResponse> getAllBancos() {
        // Devuelve una lista de bancosDTO para no devolver entidades del modelo directamente al frontend
        return bancoRepository.findAll()
            .stream()
            .map(BancoResponse::fromBanco)
            .toList();
    }

    public BancoResponse getBancoById(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        Optional<Banco> bancoOptional = bancoRepository.findById(id);
        if (bancoOptional.isEmpty()) {
            throw new EntityNotFoundException("Banco no encontrado con ID: " + id);
        }

        return BancoResponse.fromBanco(bancoOptional.get());
    }

    public BancoResponse createBanco(BancoRequest bancoDTO) {
        if(bancoDTO == null) {
            throw new IllegalArgumentException("El banco no puede ser nulo");
        }
        // Convertir el DTO a entidad
        Banco banco = bancoDTO.toBanco();
        Banco bancoGuardado = bancoRepository.save(banco);
        return BancoResponse.fromBanco(bancoGuardado);
    }

    public BancoResponse updateBanco(Integer id, BancoRequest bancoDTO) {
        if(id == null || bancoDTO == null) {
            throw new IllegalArgumentException("El ID del banco y el DTO no pueden ser nulos");
        }
        // Buscar el banco por ID
        BancoResponse bancoGetDTO = getBancoById(id);
        // Actualizar los campos del banco
        Banco banco = bancoGetDTO.toBanco();    
        banco.setNombre(bancoDTO.nombre());

        Banco bancoActualizado = bancoRepository.save(banco);
        return BancoResponse.fromBanco(bancoActualizado);
    }

    public void deleteBanco(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        BancoResponse bancoGetDTO = getBancoById(id);
        // Eliminar el banco
        bancoRepository.delete(bancoGetDTO.toBanco());
    }

    public List<BancoResponse> getBancosByNombre(String nombre) {
        if(nombre == null || nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre del banco no puede ser nulo o vacío");
        }
        
        // Filtra los bancos por nombre (ignorando mayúsculas y minúsculas)
        return bancoRepository.findByNombreContainingIgnoreCase(nombre)
            .stream()
            .map(BancoResponse::fromBanco)
            .toList();
    }
}
