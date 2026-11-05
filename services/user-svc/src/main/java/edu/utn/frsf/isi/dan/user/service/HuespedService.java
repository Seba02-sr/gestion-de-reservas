package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.HuespedUpdateRequest;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HuespedService {

  @Autowired private HuespedRepository huespedRepository;

  @Autowired private HuespedMapper huespedMapper;

  public List<HuespedResponse> getAllHuespedes() {
    return huespedRepository.findAll().stream().map(huespedMapper::toResponse).toList();
  }

  public HuespedResponse getHuespedById(Integer id) {
    Huesped huesped = getHuespedEntityById(id);
    return huespedMapper.toResponse(huesped);
  }

  @Transactional
  public HuespedResponse actualizarHuesped(Integer id, HuespedUpdateRequest request) {
    if (id == null || request == null) {
      throw new IllegalArgumentException("El id del huésped y el DTO no pueden ser nulos");
    }

    Huesped existente = getHuespedEntityById(id);
    existente.marcarComoModificado();
    huespedMapper.updateEntityFromRequest(request, existente);

    Huesped actualizado = huespedRepository.save(existente);
    return huespedMapper.toResponse(actualizado);
  }

  /**
   * Borrado físico: las tarjetas se eliminan en cascada por la relación JPA (cascade ALL +
   * orphanRemoval en Huesped.tarjetaCredito), no por una FK de la base.
   */
  @Transactional
  public void eliminarHuesped(Integer id) {
    Huesped huesped = getHuespedEntityById(id);
    huespedRepository.delete(huesped);
  }

  public List<HuespedResponse> buscarPorNombre(String nombre) {
    return huespedRepository.findByNombreContainingIgnoreCase(nombre).stream()
        .map(huespedMapper::toResponse)
        .toList();
  }

  public HuespedResponse buscarPorDni(String dni) {
    return huespedRepository
        .findByDni(dni)
        .map(huespedMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Huésped no encontrado con DNI: " + dni));
  }

  public Huesped getHuespedEntityById(Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID del huésped no puede ser nulo");
    }
    return huespedRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Huésped no encontrado con ID: " + id));
  }
}
