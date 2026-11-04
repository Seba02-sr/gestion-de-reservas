package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.TipoHabitacionMapper;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoHabitacionService {

  @Autowired private TipoHabitacionRepository tipoHabitacionRepository;

  @Autowired private HabitacionRepository habitacionRepository;

  @Autowired private TarifaRepository tarifaRepository;

  @Autowired private TipoHabitacionMapper tipoHabitacionMapper;

  @Transactional
  public TipoHabitacionResponse crear(TipoHabitacionRequest request) {
    TipoHabitacion guardado = tipoHabitacionRepository.save(tipoHabitacionMapper.toEntity(request));
    return tipoHabitacionMapper.toResponse(guardado);
  }

  @Transactional(readOnly = true)
  public TipoHabitacionResponse obtener(Integer id) {
    return tipoHabitacionMapper.toResponse(getOrThrow(id));
  }

  @Transactional(readOnly = true)
  public List<TipoHabitacionResponse> listar() {
    return tipoHabitacionRepository.findAll().stream()
        .map(tipoHabitacionMapper::toResponse)
        .toList();
  }

  @Transactional
  public TipoHabitacionResponse actualizar(Integer id, TipoHabitacionRequest request) {
    TipoHabitacion tipo = getOrThrow(id);
    tipoHabitacionMapper.updateEntityFromRequest(request, tipo);
    return tipoHabitacionMapper.toResponse(tipoHabitacionRepository.save(tipo));
  }

  /** No se puede borrar un tipo que tiene habitaciones o tarifas (409). */
  @Transactional
  public void eliminar(Integer id) {
    TipoHabitacion tipo = getOrThrow(id);
    if (habitacionRepository.existsByTipoHabitacionId(id)) {
      throw new IllegalStateException(
          "No se puede eliminar el tipo de habitación " + id + ": tiene habitaciones");
    }
    if (tarifaRepository.existsByTipoHabitacionId(id)) {
      throw new IllegalStateException(
          "No se puede eliminar el tipo de habitación " + id + ": tiene tarifas");
    }
    tipoHabitacionRepository.delete(tipo);
  }

  private TipoHabitacion getOrThrow(Integer id) {
    return tipoHabitacionRepository
        .findById(id)
        .orElseThrow(
            () -> new EntityNotFoundException("No existe un tipo de habitación con ID " + id));
  }
}
