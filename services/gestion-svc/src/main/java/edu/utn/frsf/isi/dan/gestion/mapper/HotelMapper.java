package edu.utn.frsf.isi.dan.gestion.mapper;

import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.dto.HotelUpdateRequest;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HotelMapper {

  /**
   * Convierte HotelRequest a Hotel Entity El ID se setea como null automaticamente para nuevas
   * entidades
   *
   * @param request
   * @return
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "habitaciones", expression = "java(new java.util.ArrayList<>())")
  @Mapping(target = "amenities", expression = "java(new java.util.ArrayList<>())")
  @Mapping(target = "cerrado", expression = "java(false)")
  Hotel toEntity(HotelRequest request);

  /**
   * Convierte Hotel Entity a HotelResponse
   *
   * @param hotel
   * @return
   */
  HotelResponse toResponse(Hotel hotel);

  /**
   * Actualiza un hotel existente. Solo se pueden editar categoría, teléfono y correo de contacto
   * (los demás datos del hotel no cambian).
   *
   * @param request
   * @param hotel
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "nombre", ignore = true)
  @Mapping(target = "cuit", ignore = true)
  @Mapping(target = "domicilio", ignore = true)
  @Mapping(target = "latitud", ignore = true)
  @Mapping(target = "longitud", ignore = true)
  @Mapping(target = "cerrado", ignore = true)
  @Mapping(target = "habitaciones", ignore = true)
  @Mapping(target = "amenities", ignore = true)
  void updateEntityFromRequest(HotelUpdateRequest request, @MappingTarget Hotel hotel);
}
