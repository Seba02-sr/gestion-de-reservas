package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.TarjetaCreditoRepository;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.mapper.TarjetaCreditoMapper;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TarjetaCreditoService {

  @Autowired public TarjetaCreditoRepository tarjetaCreditoRepository;

  @Autowired public TarjetaCreditoMapper tarjetaCreditoMapper;

  public List<TarjetaCreditoResponse> getAllTarjetas() {
    return tarjetaCreditoRepository.findAll().stream()
        .map(tarjetaCreditoMapper::toResponse)
        .toList();
  }

  public TarjetaCreditoResponse getTarjetaById(Long id) {
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

  // public TarjetaCreditoResponse crearTarhetaParaHuesped(Long huespedId, TarjetaCreditoRequest
  // tarjetaCreditoRequest){
  //     if (tarjetaCreditoRequest == null){
  //         throw new IllegalArgumentException("La tarjeta no puede ser nula");
  //     }

  //     TarjetaCredito tarjetaCredito = tarjetaCreditoMapper.toEntity(tarjetaCreditoRequest);
  //     Huesped huesped = huespedService.buscarHuespedPorId(huespedId);
  //     tarjetaCredito.setHuesped(huesped);
  //     tarjetaCreditoRepository.save(tarjetaCredito);
  //     return tarjetaCreditoMapper.toResponse(tarjetaCredito);
  //
  // }

  public TarjetaCreditoResponse updateTarjetaCredito(
      Long id, TarjetaCreditoRequest tarjetaCreditoRequest) {
    if (id == null || tarjetaCreditoRequest == null) {
      throw new IllegalArgumentException("El id de la tarjeta y el DTO no pueden ser nulos");
    }

    TarjetaCredito tarjetaCreditoExistente = getTarjetaEntityById(id);
    tarjetaCreditoMapper.updateEntityFromRequest(tarjetaCreditoRequest, tarjetaCreditoExistente);
    TarjetaCredito tarjetaCreditoActualizada =
        tarjetaCreditoRepository.save(tarjetaCreditoExistente);

    return tarjetaCreditoMapper.toResponse(tarjetaCreditoActualizada);
  }

  public void deleteTarjetaCredito(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID de la tarjeta no puede ser nula");
    }

    TarjetaCredito tarjetaCredito = getTarjetaEntityById(id);

    if (Boolean.TRUE.equals(tarjetaCredito.isPrincipal())) {
      throw new IllegalStateException("No se puede eliminar la tarjeta principal");
    }

    tarjetaCreditoRepository.delete(tarjetaCredito);
  }

  public TarjetaCredito getTarjetaEntityById(Long id) {
    return tarjetaCreditoRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Tarjeta no encontrada con ID: " + id));
  }

  public TarjetaCreditoResponse setTarjetaPrincipal(Long id) {
    TarjetaCredito tarjetaCredito = getTarjetaEntityById(id);

    List<TarjetaCredito> otrasTarjetas =
        tarjetaCreditoRepository.findByHuespedIdAndIdNot(tarjetaCredito.getHuesped().getId(), id);

    otrasTarjetas.forEach(t -> t.setEsPrincipal(false));
    tarjetaCreditoRepository.saveAll(otrasTarjetas);

    tarjetaCredito.setEsPrincipal(true);
    TarjetaCredito tarjetaCreditoGuardada = tarjetaCreditoRepository.save(tarjetaCredito);

    return tarjetaCreditoMapper.toResponse(tarjetaCreditoGuardada);
  }
}
