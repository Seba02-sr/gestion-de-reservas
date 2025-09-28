package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HuespedService {

  @Autowired private HuespedRepository huespedRepository;

  @Autowired private HuespedMapper huespedMapper;

  public List<HuespedResponse> getAllHuespedes() {
    return huespedRepository.findByActivoTrue().stream().map(huespedMapper::toResponse).toList();
  }

  public HuespedResponse getHuespedById(Long id) {
    Huesped huesped = getHuespedEntityById(id);
    return huespedMapper.toResponse(huesped);
  }

  public HuespedResponse actualizarHuesped(Long id, HuespedRequest request) {
    if (id == null || request == null) {
      throw new IllegalArgumentException("El id del huésped y el DTO no pueden ser nulos");
    }

    Huesped existente = getHuespedEntityById(id);
    existente.marcarComoModificado();
    huespedMapper.updateEntityFromRequest(request, existente);

    // Asegurar relación inversa huesped <- tarjetas
    if (existente.getTarjetaCredito() != null) {
      existente.getTarjetaCredito().forEach(t -> t.setHuesped(existente));
    }

    Huesped actualizado = huespedRepository.save(existente);
    return huespedMapper.toResponse(actualizado);
  }

  public void eliminarHuesped(Long id) {
    Huesped huesped = getHuespedEntityById(id);
    huesped.marcarComoEliminado();
    huespedRepository.save(huesped);
  }

  public List<HuespedResponse> buscarPorNombre(String nombre) {
    return huespedRepository.findByActivoTrueAndNombreContainingIgnoreCase(nombre).stream()
        .map(huespedMapper::toResponse)
        .toList();
  }

  public HuespedResponse buscarPorDni(String dni) {
    return huespedRepository
        .findByActivoTrueAndDni(dni)
        .map(huespedMapper::toResponse)
        .orElseThrow(() -> new EntityNotFoundException("Huésped no encontrado con DNI: " + dni));
  }

  public Huesped getHuespedEntityById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID del huésped no puede ser nulo");
    }
    return huespedRepository
        .findByIdAndActivoTrue(id)
        .orElseThrow(() -> new EntityNotFoundException("Huésped no encontrado con ID: " + id));
  }
}
