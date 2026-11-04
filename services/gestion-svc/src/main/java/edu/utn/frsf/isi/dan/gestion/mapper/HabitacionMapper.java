package edu.utn.frsf.isi.dan.gestion.mapper;

import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HabitacionMapper {

  /** El hotel y el tipo de habitación los resuelve el service (hay que buscarlos en la base). */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true)
  @Mapping(target = "tipoHabitacion", ignore = true)
  @Mapping(
      target = "disponible",
      expression = "java(request.disponible() == null ? Boolean.TRUE : request.disponible())")
  Habitacion toEntity(HabitacionRequest request);

  @Mapping(target = "idTipoHabitacion", source = "tipoHabitacion.id")
  @Mapping(target = "tipoHabitacion", source = "tipoHabitacion.nombre")
  @Mapping(target = "idHotel", source = "hotel.id")
  HabitacionResponse toResponse(Habitacion habitacion);

  /** Actualiza número, piso y disponibilidad; el hotel y el tipo los resuelve el service. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true)
  @Mapping(target = "tipoHabitacion", ignore = true)
  void updateEntityFromRequest(HabitacionRequest request, @MappingTarget Habitacion habitacion);
}
