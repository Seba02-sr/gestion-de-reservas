package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.TarjetaCreditoRepository;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.mapper.TarjetaCreditoMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarjetaCreditoService {

  @Autowired private TarjetaCreditoRepository tarjetaCreditoRepository;

  @Autowired private TarjetaCreditoMapper tarjetaCreditoMapper;

  @Autowired private HuespedService huespedService;

  public List<TarjetaCreditoResponse> getAllTarjetas() {
    return tarjetaCreditoRepository.findAll().stream()
        .map(tarjetaCreditoMapper::toResponse)
        .toList();
  }

  public TarjetaCreditoResponse getTarjetaById(Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID de la tarjeta no puede ser nulo");
    }
    TarjetaCredito tarjetaCredito = getTarjetaEntityById(id);
    return tarjetaCreditoMapper.toResponse(tarjetaCredito);
  }

  public TarjetaCreditoResponse crearTarjeta(TarjetaCreditoRequest tarjetaCreditoRequest) {
    if (tarjetaCreditoRequest == null) {
      throw new IllegalArgumentException("La tarjeta no puede ser nula");
    }

    TarjetaCredito tarjetaCredito = tarjetaCreditoMapper.toEntity(tarjetaCreditoRequest);
    TarjetaCredito tarjetaCreditoGuardada = tarjetaCreditoRepository.save(tarjetaCredito);
    return tarjetaCreditoMapper.toResponse(tarjetaCreditoGuardada);
  }

  @Transactional
  public TarjetaCreditoResponse crearTarjetaParaHuesped(
      Integer huespedId, TarjetaCreditoRequest tarjetaCreditoRequest) {
    if (huespedId == null || tarjetaCreditoRequest == null) {
      throw new IllegalArgumentException("El id del huésped y la tarjeta no pueden ser nulos");
    }

    Huesped huesped = huespedService.getHuespedEntityById(huespedId);
    TarjetaCredito tarjetaCredito = tarjetaCreditoMapper.toEntity(tarjetaCreditoRequest);
    tarjetaCredito.setHuesped(huesped);

    if (tarjetaCredito.isPrincipal()) {
      desmarcarPrincipales(huespedId, null);
    }

    TarjetaCredito guardada = tarjetaCreditoRepository.save(tarjetaCredito);
    return tarjetaCreditoMapper.toResponse(guardada);
  }

  @Transactional
  public TarjetaCreditoResponse updateTarjetaCredito(
      Integer id, TarjetaCreditoRequest tarjetaCreditoRequest) {
    if (id == null || tarjetaCreditoRequest == null) {
      throw new IllegalArgumentException("El id de la tarjeta y el DTO no pueden ser nulos");
    }

    TarjetaCredito tarjetaCreditoExistente = getTarjetaEntityById(id);

    boolean eraPrincipal = tarjetaCreditoExistente.isPrincipal();
    boolean pidePrincipal = Boolean.TRUE.equals(tarjetaCreditoRequest.esPrincipal());
    boolean pideNoPrincipal = Boolean.FALSE.equals(tarjetaCreditoRequest.esPrincipal());

    if (eraPrincipal && pideNoPrincipal) {
      throw new IllegalStateException(
          "No se puede quitar la marca de principal; usar set-principal sobre otra tarjeta");
    }
    if (!eraPrincipal && pidePrincipal && tarjetaCreditoExistente.getHuesped() != null) {
      desmarcarPrincipales(tarjetaCreditoExistente.getHuesped().getId(), id);
    }
    tarjetaCreditoMapper.updateEntityFromRequest(tarjetaCreditoRequest, tarjetaCreditoExistente);
    TarjetaCredito tarjetaCreditoActualizada =
        tarjetaCreditoRepository.save(tarjetaCreditoExistente);

    return tarjetaCreditoMapper.toResponse(tarjetaCreditoActualizada);
  }

  public void deleteTarjetaCredito(Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID de la tarjeta no puede ser nula");
    }

    TarjetaCredito tarjetaCredito = getTarjetaEntityById(id);

    if (Boolean.TRUE.equals(tarjetaCredito.isPrincipal())) {
      throw new IllegalStateException("No se puede eliminar la tarjeta principal");
    }

    tarjetaCreditoRepository.delete(tarjetaCredito);
  }

  public TarjetaCredito getTarjetaEntityById(Integer id) {
    return tarjetaCreditoRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada con ID: " + id));
  }

  @Transactional
  public TarjetaCreditoResponse setTarjetaPrincipal(Integer id) {
    TarjetaCredito tarjetaCredito = getTarjetaEntityById(id);

    desmarcarPrincipales(tarjetaCredito.getHuesped().getId(), id);

    tarjetaCredito.setEsPrincipal(true);
    TarjetaCredito tarjetaCreditoGuardada = tarjetaCreditoRepository.save(tarjetaCredito);

    return tarjetaCreditoMapper.toResponse(tarjetaCreditoGuardada);
  }

  public java.util.List<TarjetaCreditoResponse> getTarjetasPorHuesped(Integer huespedId) {
    return tarjetaCreditoRepository.findByHuespedId(huespedId).stream()
        .map(tarjetaCreditoMapper::toResponse)
        .toList();
  }

  /** Desmarca las tarjetas principales del huésped, excepto {@code excluirId} (puede ser null). */
  private void desmarcarPrincipales(Integer huespedId, Integer excluirId) {
    List<TarjetaCredito> otras =
        excluirId == null
            ? tarjetaCreditoRepository.findByHuespedId(huespedId)
            : tarjetaCreditoRepository.findByHuespedIdAndIdNot(huespedId, excluirId);
    otras.forEach(t -> t.setEsPrincipal(false));
    tarjetaCreditoRepository.saveAll(otras);
  }
}
