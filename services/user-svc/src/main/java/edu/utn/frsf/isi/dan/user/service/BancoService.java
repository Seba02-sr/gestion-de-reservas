package edu.utn.frsf.isi.dan.user.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.mapper.BancoMapper;
import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.persistence.EntityNotFoundException;

@Service
public class BancoService {

    @Autowired
    private BancoRepository bancoRepository;

    @Autowired
    private BancoMapper bancoMapper;

    public List<BancoResponse> getAllBancos() {
        // Devuelve una lista de bancosDTO para no devolver entidades del modelo directamente al frontend
        return bancoRepository.findAll()
            .stream()
            .map(bancoMapper::toResponse)
            .toList();
    }

    public BancoResponse getBancoById(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        Banco banco = getBancoEntityById(id);
        return bancoMapper.toResponse(banco);
    }

    public BancoResponse createBanco(BancoRequest bancoRequest) {
        if(bancoRequest == null) {
            throw new IllegalArgumentException("El banco no puede ser nulo");
        }
        // Convertir el DTO a entidad
        Banco banco = bancoMapper.toEntity(bancoRequest);
        Banco bancoGuardado = bancoRepository.save(banco);
        return bancoMapper.toResponse(bancoGuardado);
    }

    public BancoResponse updateBanco(Integer id, BancoRequest bancoRequest) {
        if(id == null || bancoRequest == null) {
            throw new IllegalArgumentException("El ID del banco y el DTO no pueden ser nulos");
        }
        // Buscar el banco por ID
        Banco bancoExistente = getBancoEntityById(id);
        // Actualizar los campos del banco
        bancoMapper.updateEntityFromRequest(bancoRequest, bancoExistente);
        Banco bancoActualizado = bancoRepository.save(bancoExistente);

        return bancoMapper.toResponse(bancoActualizado);
    }

    public void deleteBanco(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        Banco banco = getBancoEntityById(id);
        // Eliminar el banco
        bancoRepository.delete(banco);
    }

    public List<BancoResponse> getBancosByNombre(String nombre) {
        if(nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del banco no puede ser nulo o vacío");
        }
        
        // Filtra los bancos por nombre (ignorando mayúsculas y minúsculas)
        return bancoRepository.findByNombreContainingIgnoreCase(nombre)
            .stream()
            .map(bancoMapper::toResponse)
            .toList();
    }

    public Banco getBancoEntityById(Integer id) {
        return bancoRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Banco no encontrado con ID: " + id));
    }
}
