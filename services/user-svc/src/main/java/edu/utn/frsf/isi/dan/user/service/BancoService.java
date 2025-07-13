package edu.utn.frsf.isi.dan.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dto.BancoRecord;
import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.persistence.EntityNotFoundException;

@Service
public class BancoService {

    @Autowired
    private BancoRepository bancoRepository;

    public List<Banco> getAllBancos() {
        return bancoRepository.findAll();
    }

    public Banco getBancoById(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        Optional<Banco> bancoOptional = bancoRepository.findById(id);
        if (bancoOptional.isEmpty()) {
            throw new EntityNotFoundException("Banco no encontrado con ID: " + id);
        }

        Banco banco = bancoOptional.get();
        return banco;
    }

    public Banco createBanco(BancoRecord bancoDTO) {
        if(bancoDTO == null) {
            throw new IllegalArgumentException("El banco no puede ser nulo");
        }
        // Convertir el DTO a entidad
        Banco banco = bancoDTO.toBanco();
        return bancoRepository.save(banco);
    }

    public Banco updateBanco(Integer id, BancoRecord bancoDTO) {
        if(id == null || bancoDTO == null) {
            throw new IllegalArgumentException("El ID del banco y el DTO no pueden ser nulos");
        }
        // Buscar el banco por ID
        Banco banco = getBancoById(id);
        // Actualizar los campos del banco
        banco.setNombre(bancoDTO.nombre());
        return bancoRepository.save(banco);
    }

    public void deleteBanco(Integer id) {
        if(id == null) {
            throw new IllegalArgumentException("El ID del banco no puede ser nulo");
        }
        // Buscar el banco por ID
        Banco banco = getBancoById(id);
        // Eliminar el banco
        bancoRepository.delete(banco);
    }
}
