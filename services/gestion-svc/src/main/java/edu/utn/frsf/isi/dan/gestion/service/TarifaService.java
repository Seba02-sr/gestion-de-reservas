package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.TarifaMapper;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TarifaDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio de las tarifas (ETAPA 02). Las tarifas solo se crean o se eliminan; por cada
 * tipo de habitación forman una línea de tiempo sin huecos ni superposiciones.
 *
 * <p>Tarifa vigente en una fecha = {@code fechaInicio <= fecha} y ({@code fechaFin} es null o
 * {@code fechaFin >= fecha}).
 *
 * <p>Cada vez que cambia el precio vigente "de hoy" se publica un {@code ACTUALIZAR_PRECIO}
 * (después del commit). Los cambios que ocurren en una fecha futura (p. ej. cuando empieza o
 * termina una promoción) los detecta la tarea diaria {@link #publicarCambiosDePrecioDelDia()}.
 */
@Service
@Log4j2
public class TarifaService {

  @Autowired private TarifaRepository tarifaRepository;

  @Autowired private TipoHabitacionRepository tipoHabitacionRepository;

  @Autowired private TarifaMapper tarifaMapper;

  @Autowired private ApplicationEventPublisher eventPublisher;

  @Autowired private Clock clock;

  // ------------------------------------------------------------------ consultas

  /** Tarifa vigente de un tipo de habitación en {@code fecha}. */
  public Optional<Tarifa> obtenerTarifaVigente(Integer tipoHabitacionId, LocalDate fecha) {
    return tarifaRepository.findVigentes(tipoHabitacionId, fecha).stream().findFirst();
  }

  /** Tarifa vigente hoy (la usan las habitaciones para informar el precio). */
  public Optional<Tarifa> obtenerTarifaVigente(Integer tipoHabitacionId) {
    return obtenerTarifaVigente(tipoHabitacionId, hoy());
  }

  /** Igual que {@link #obtenerTarifaVigente(Integer, LocalDate)} pero 404 si no hay ninguna. */
  @Transactional(readOnly = true)
  public TarifaResponse vigente(Integer tipoHabitacionId, LocalDate fecha) {
    LocalDate dia = fecha != null ? fecha : hoy();
    return obtenerTarifaVigente(tipoHabitacionId, dia)
        .map(tarifaMapper::toResponse)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "No hay tarifa vigente el "
                        + dia
                        + " para el tipo de habitación "
                        + tipoHabitacionId));
  }

  @Transactional(readOnly = true)
  public TarifaResponse obtener(Integer id) {
    return tarifaMapper.toResponse(getTarifaOrThrow(id));
  }

  /** Lista las tarifas (de un tipo si se indica), ordenadas por fecha de inicio. */
  @Transactional(readOnly = true)
  public List<TarifaResponse> listar(Integer tipoHabitacionId) {
    List<Tarifa> tarifas =
        tipoHabitacionId != null
            ? tarifaRepository.findByTipoHabitacionIdOrderByFechaInicioAsc(tipoHabitacionId)
            : tarifaRepository.findAll().stream()
                .sorted(
                    Comparator.comparing((Tarifa t) -> t.getTipoHabitacion().getId())
                        .thenComparing(Tarifa::getFechaInicio))
                .toList();
    return tarifas.stream().map(tarifaMapper::toResponse).toList();
  }

  // ----------------------------------------------------------------------- alta

  /**
   * Crea una tarifa.
   *
   * <ul>
   *   <li>Sin fechas: la tarifa actual del tipo pasa a terminar ayer y la nueva rige desde hoy sin
   *       fecha de fin.
   *   <li>Con inicio y fin: tarifa especial. La tarifa actual termina el día anterior al inicio, se
   *       crea la promoción y se crea otra tarifa (mismo precio que la que se cortó) desde el día
   *       siguiente al fin de la promoción.
   * </ul>
   */
  @Transactional
  public TarifaResponse crear(TarifaRequest request) {
    TipoHabitacion tipo =
        tipoHabitacionRepository
            .findById(request.idTipoHabitacion())
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No existe un tipo de habitación con ID " + request.idTipoHabitacion()));

    boolean sinFechas = request.fechaInicio() == null && request.fechaFin() == null;
    boolean conAmbasFechas = request.fechaInicio() != null && request.fechaFin() != null;
    if (!sinFechas && !conAmbasFechas) {
      throw new IllegalArgumentException(
          "Para una tarifa especial hay que informar fechaInicio y fechaFin; para una tarifa común, ninguna");
    }

    LocalDate hoy = hoy();
    List<Tarifa> tarifas =
        new ArrayList<>(tarifaRepository.findByTipoHabitacionIdOrderByFechaInicioAsc(tipo.getId()));
    Double precioAntes = precioVigenteEn(tarifas, hoy);

    Tarifa creada =
        sinFechas
            ? crearComun(request, tipo, tarifas, hoy)
            : crearEspecial(request, tipo, tarifas, hoy);

    publicarSiCambioElPrecioDeHoy(tipo.getId(), precioAntes, precioVigenteEn(tarifas, hoy));
    return tarifaMapper.toResponse(creada);
  }

  private Tarifa crearComun(
      TarifaRequest request, TipoHabitacion tipo, List<Tarifa> tarifas, LocalDate hoy) {
    if (tarifas.stream().anyMatch(t -> t.getFechaInicio().isAfter(hoy))) {
      throw new IllegalStateException(
          "El tipo de habitación "
              + tipo.getId()
              + " tiene tarifas futuras (p. ej. una promoción programada): eliminelas antes de crear una tarifa común");
    }

    Optional<Tarifa> actual = vigenteEn(tarifas, hoy);
    if (actual.isPresent()) {
      Tarifa t = actual.get();
      if (t.getFechaInicio().isBefore(hoy)) {
        t.setFechaFin(hoy.minusDays(1));
        tarifaRepository.save(t);
      } else {
        // Empezó hoy: cerrarla "ayer" la dejaría con fin < inicio. La nueva la reemplaza.
        tarifaRepository.delete(t);
        tarifas.removeIf(x -> x == t);
      }
    }

    Tarifa nueva =
        tarifaRepository.save(
            Tarifa.builder()
                .nombre(request.nombre())
                .tipoHabitacion(tipo)
                .precioNoche(request.precioNoche())
                .fechaInicio(hoy)
                .fechaFin(null)
                .especial(false)
                .build());
    tarifas.add(nueva);
    log.info("Tarifa {} creada para el tipo {} desde {}", nueva.getId(), tipo.getId(), hoy);
    return nueva;
  }

  private Tarifa crearEspecial(
      TarifaRequest request, TipoHabitacion tipo, List<Tarifa> tarifas, LocalDate hoy) {
    LocalDate inicio = request.fechaInicio();
    LocalDate fin = request.fechaFin();
    if (inicio.isAfter(fin)) {
      throw new IllegalArgumentException("fechaInicio no puede ser posterior a fechaFin");
    }
    if (inicio.isBefore(hoy)) {
      throw new IllegalArgumentException("fechaInicio no puede ser anterior a hoy (" + hoy + ")");
    }

    boolean seSuperpone =
        tarifas.stream()
            .filter(t -> Boolean.TRUE.equals(t.getEspecial()))
            .anyMatch(t -> !t.getFechaInicio().isAfter(fin) && !t.getFechaFin().isBefore(inicio));
    if (seSuperpone) {
      throw new IllegalStateException(
          "La promoción se superpone con otra tarifa especial del tipo " + tipo.getId());
    }

    Tarifa base =
        tarifas.stream()
            .filter(t -> !Boolean.TRUE.equals(t.getEspecial()))
            .filter(t -> !t.getFechaInicio().isAfter(inicio))
            .filter(t -> t.getFechaFin() == null || !t.getFechaFin().isBefore(inicio))
            .max(Comparator.comparing(Tarifa::getFechaInicio))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No hay una tarifa vigente el "
                            + inicio
                            + " para el tipo "
                            + tipo.getId()
                            + ": primero hay que crear una tarifa común"));
    LocalDate finOriginalDeLaBase = base.getFechaFin();
    if (finOriginalDeLaBase != null && finOriginalDeLaBase.isBefore(fin)) {
      throw new IllegalStateException(
          "La promoción excede la vigencia de la tarifa "
              + base.getId()
              + " (hasta "
              + finOriginalDeLaBase
              + ")");
    }

    // a. la tarifa actual queda con fin = día anterior al inicio de la promo
    if (base.getFechaInicio().isBefore(inicio)) {
      base.setFechaFin(inicio.minusDays(1));
      tarifaRepository.save(base);
    } else {
      // La base empezaba el mismo día que la promo: no queda ningún día suyo antes de ella.
      // La tarifa de continuación (c) conserva su precio.
      tarifaRepository.delete(base);
      tarifas.removeIf(x -> x == base);
    }

    // b. la promoción
    Tarifa promo =
        tarifaRepository.save(
            Tarifa.builder()
                .nombre(request.nombre())
                .tipoHabitacion(tipo)
                .precioNoche(request.precioNoche())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .especial(true)
                .build());
    tarifas.add(promo);

    // c. continuación: día siguiente al fin de la promo, mismo precio que la tarifa que se cortó
    LocalDate inicioContinuacion = fin.plusDays(1);
    if (finOriginalDeLaBase == null || !finOriginalDeLaBase.isBefore(inicioContinuacion)) {
      tarifas.add(
          tarifaRepository.save(
              Tarifa.builder()
                  .nombre(base.getNombre())
                  .tipoHabitacion(tipo)
                  .precioNoche(base.getPrecioNoche())
                  .fechaInicio(inicioContinuacion)
                  .fechaFin(finOriginalDeLaBase)
                  .especial(false)
                  .build()));
    }
    log.info(
        "Promoción {} creada para el tipo {}: {} a {}", promo.getId(), tipo.getId(), inicio, fin);
    return promo;
  }

  // ----------------------------------------------------------------------- baja

  /**
   * Elimina una tarifa. Solo se puede si el tipo de habitación tiene otra tarifa (409 si es la
   * única).
   *
   * <p>Criterio para no dejar huecos: si la tarifa borrada es la vigente o una futura, la tarifa
   * anterior (la de inicio más cercano hacia atrás) extiende su fecha de fin hasta donde terminaba
   * la borrada (null si era abierta). Así la anterior pasa a ser la vigente sin días sin precio. Si
   * la borrada ya terminó (es del pasado) simplemente se borra, para no reescribir el historial.
   */
  @Transactional
  public void eliminar(Integer id) {
    Tarifa tarifa = getTarifaOrThrow(id);
    Integer tipoId = tarifa.getTipoHabitacion().getId();
    List<Tarifa> tarifas =
        new ArrayList<>(tarifaRepository.findByTipoHabitacionIdOrderByFechaInicioAsc(tipoId));
    if (tarifas.size() <= 1) {
      throw new IllegalStateException(
          "No se puede eliminar la tarifa "
              + id
              + ": es la única del tipo de habitación "
              + tipoId
              + " (tiene que existir otra tarifa)");
    }

    LocalDate hoy = hoy();
    Double precioAntes = precioVigenteEn(tarifas, hoy);
    boolean esVigente =
        vigenteEn(tarifas, hoy).map(v -> Objects.equals(v.getId(), id)).orElse(false);
    boolean esPasada = tarifa.getFechaFin() != null && tarifa.getFechaFin().isBefore(hoy);

    if (!esPasada) {
      Optional<Tarifa> anterior =
          tarifas.stream()
              .filter(t -> t.getFechaInicio().isBefore(tarifa.getFechaInicio()))
              .max(Comparator.comparing(Tarifa::getFechaInicio));
      if (anterior.isPresent()) {
        Tarifa prev = anterior.get();
        prev.setFechaFin(tarifa.getFechaFin());
        if (prev.getFechaFin() == null) {
          // una promoción que queda abierta deja de ser "especial": es la tarifa de ahí en más
          prev.setEspecial(false);
        }
        tarifaRepository.save(prev);
      } else if (esVigente) {
        throw new IllegalStateException(
            "No se puede eliminar la tarifa vigente "
                + id
                + ": no existe una tarifa anterior que pase a ser la vigente");
      }
    }

    tarifaRepository.delete(tarifa);
    tarifas.removeIf(t -> t == tarifa);
    log.info("Tarifa {} eliminada (vigente: {})", id, esVigente);

    publicarSiCambioElPrecioDeHoy(tipoId, precioAntes, precioVigenteEn(tarifas, hoy));
  }

  // --------------------------------------------------------------- tarea diaria

  /**
   * Publica ACTUALIZAR_PRECIO para cada tipo de habitación cuya tarifa vigente cambió hoy, es
   * decir, que tiene una tarifa que empieza hoy y cuyo precio difiere del que regía ayer. Cubre el
   * inicio de una promoción y el regreso al precio común cuando termina. La invoca la tarea
   * programada {@code TarifaPrecioScheduler}.
   *
   * @return cantidad de tipos de habitación para los que se publicó un evento
   */
  @Transactional
  public int publicarCambiosDePrecioDelDia() {
    LocalDate hoy = hoy();
    LocalDate ayer = hoy.minusDays(1);

    Set<Integer> tipos = new LinkedHashSet<>();
    tarifaRepository.findByFechaInicio(hoy).forEach(t -> tipos.add(t.getTipoHabitacion().getId()));

    int publicados = 0;
    for (Integer tipoId : tipos) {
      Optional<Tarifa> actual = obtenerTarifaVigente(tipoId, hoy);
      Optional<Tarifa> anterior = obtenerTarifaVigente(tipoId, ayer);
      if (actual.isPresent()
          && (anterior.isEmpty()
              || !Objects.equals(anterior.get().getPrecioNoche(), actual.get().getPrecioNoche()))) {
        publicarPrecio(tipoId, actual.get().getPrecioNoche());
        publicados++;
      }
    }
    log.info("Cambios de precio del {}: {} tipo(s) de habitación notificados", hoy, publicados);
    return publicados;
  }

  // ------------------------------------------------------------------- helpers

  private LocalDate hoy() {
    return LocalDate.now(clock);
  }

  private Tarifa getTarifaOrThrow(Integer id) {
    return tarifaRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("No existe una tarifa con ID " + id));
  }

  private static Optional<Tarifa> vigenteEn(List<Tarifa> tarifas, LocalDate fecha) {
    return tarifas.stream()
        .filter(t -> !t.getFechaInicio().isAfter(fecha))
        .filter(t -> t.getFechaFin() == null || !t.getFechaFin().isBefore(fecha))
        .max(Comparator.comparing(Tarifa::getFechaInicio));
  }

  private static Double precioVigenteEn(List<Tarifa> tarifas, LocalDate fecha) {
    return vigenteEn(tarifas, fecha).map(Tarifa::getPrecioNoche).orElse(null);
  }

  private void publicarSiCambioElPrecioDeHoy(Integer tipoId, Double antes, Double despues) {
    if (despues != null && !Objects.equals(antes, despues)) {
      publicarPrecio(tipoId, despues);
    }
  }

  private void publicarPrecio(Integer tipoId, Double nuevoPrecio) {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(tipoId).nuevoPrecio(nuevoPrecio).build())
            .build();
    eventPublisher.publishEvent(new HabitacionEventoPublicado(evento));
  }
}
