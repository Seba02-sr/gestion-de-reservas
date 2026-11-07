package edu.utn.frsf.isi.dan.gestion.mapper;

import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TarifaMapper {

  @Mapping(target = "idTipoHabitacion", source = "tipoHabitacion.id")
  TarifaResponse toResponse(Tarifa tarifa);
}
