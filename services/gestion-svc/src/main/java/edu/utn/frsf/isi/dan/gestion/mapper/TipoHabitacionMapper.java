package edu.utn.frsf.isi.dan.gestion.mapper;

import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TipoHabitacionMapper {

  @Mapping(target = "id", ignore = true)
  TipoHabitacion toEntity(TipoHabitacionRequest request);

  TipoHabitacionResponse toResponse(TipoHabitacion tipoHabitacion);

  @Mapping(target = "id", ignore = true)
  void updateEntityFromRequest(
      TipoHabitacionRequest request, @MappingTarget TipoHabitacion tipoHabitacion);
}
