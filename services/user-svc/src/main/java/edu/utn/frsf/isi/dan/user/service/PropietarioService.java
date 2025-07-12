package edu.utn.frsf.isi.dan.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.utn.frsf.isi.dan.user.dao.PropietarioRepository;
import edu.utn.frsf.isi.dan.user.model.Propietario;

@Service
public class PropietarioService {

    @Autowired
    private PropietarioRepository propietarioRepository;

    public List<Propietario> getAllPropietarios() {
        return propietarioRepository.findAll();
    }

    public Optional<Propietario> getPropietarioById(Long id) {
        return propietarioRepository.findById(id);
    }

    public Propietario actualizarPropietario(Long id, Propietario actualizado) {
        return propietarioRepository.findById(id)
                .map(p -> {
                    p.setNombre(actualizado.getNombre());
                    p.setEmail(actualizado.getEmail());
                    p.setTelefono(actualizado.getTelefono());
                    return propietarioRepository.save(p);
                })
                .orElseThrow(() -> new IllegalArgumentException("Propietario no encontrado con ID: " + id));
    }

    public List<Propietario> buscarPorNombre(String nombre) {
        return propietarioRepository.findByNombreContainingIgnoreCase(nombre);
    }

    public List<Propietario> buscarPorDniParcial(String dni) {
        return propietarioRepository.findByDniStartingWith(dni);
    }

}
