package edu.utn.frsf.isi.dan.user.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.model.Huesped;

@Service
public class HuespedService {

    @Autowired
    private HuespedRepository huespedRepository;

    public List<Huesped> getAllHuespedes() {
        return huespedRepository.findAll();
    }

    public Optional<Huesped> getHuespedById(Long id) {
        return huespedRepository.findById(id);
    }

    public Huesped actualizarHuesped(Long id, Huesped huespedActualizado) {
        return huespedRepository.findById(id)
                .map(h -> {
                    h.setNombre(huespedActualizado.getNombre());
                    h.setEmail(huespedActualizado.getEmail());
                    h.setTelefono(huespedActualizado.getTelefono());
                    h.setFechaNacimiento(huespedActualizado.getFechaNacimiento());
                    return huespedRepository.save(h);
                })
                .orElseThrow(() -> new IllegalArgumentException("Huésped no encontrado con ID: " + id));
    }

    public void eliminarHuesped(Long id) {
        if (!huespedRepository.existsById(id)) {
            throw new IllegalArgumentException("No se puede eliminar: huésped no encontrado con ID: " + id);
        }
        huespedRepository.deleteById(id);
    }

    public List<Huesped> buscarPorNombre(String nombre) {
    return huespedRepository.findByNombreContainingIgnoreCase(nombre);
    }

    public Optional<Huesped> buscarPorDni(String dni) {
        return huespedRepository.findByDni(dni);
    }
}
