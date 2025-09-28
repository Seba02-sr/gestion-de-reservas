package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.PropietarioRepository;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.mapper.PropietarioMapper;
import edu.utn.frsf.isi.dan.user.model.Propietario;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PropietarioService {

  @Autowired private PropietarioRepository propietarioRepository;

  @Autowired private PropietarioMapper propietarioMapper;

  public List<PropietarioResponse> getAllPropietarios() {
    return propietarioRepository.findByActivoTrue().stream()
        .map(propietarioMapper::toResponse)
        .toList();
  }

  public PropietarioResponse getPropietarioById(Long id) {
    Propietario p = getPropietarioEntityById(id);
    return propietarioMapper.toResponse(p);
  }

  public PropietarioResponse actualizarPropietario(Long id, PropietarioRequest request) {
    if (id == null || request == null) {
      throw new IllegalArgumentException("El id del propietario y el DTO no pueden ser nulos");
    }
    Propietario existente = getPropietarioEntityById(id);
    existente.marcarComoModificado();
    propietarioMapper.updateEntityFromRequest(request, existente);

    // Relación inversa cuenta -> propietario
    if (existente.getCuentaBancaria() != null) {
      existente.getCuentaBancaria().setPropietario(existente);
    }

    Propietario guardado = propietarioRepository.save(existente);
    return propietarioMapper.toResponse(guardado);
  }

  public List<PropietarioResponse> buscarPorNombre(String nombre) {
    return propietarioRepository.findByActivoTrueAndNombreContainingIgnoreCase(nombre).stream()
        .map(propietarioMapper::toResponse)
        .toList();
  }

  public List<PropietarioResponse> buscarPorDniParcial(String dni) {
    return propietarioRepository.findByActivoTrueAndDniStartingWith(dni).stream()
        .map(propietarioMapper::toResponse)
        .toList();
  }

  public PropietarioResponse buscarPorDniExacto(String dni) {
    return propietarioRepository
        .findByActivoTrueAndDni(dni)
        .map(propietarioMapper::toResponse)
        .orElseThrow(
            () -> new EntityNotFoundException("Propietario no encontrado con DNI: " + dni));
  }

  public Propietario getPropietarioEntityById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID del propietario no puede ser nulo");
    }
    return propietarioRepository
        .findByIdAndActivoTrue(id)
        .orElseThrow(
            () -> new EntityNotFoundException("Propietario no encontrado con ID: " + id));
  }
}
