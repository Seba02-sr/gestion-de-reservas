package edu.utn.frsf.isi.dan.gestion.mapper;

import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelResponse;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AmenityHotelMapper {

  /**
   * Convierte AmenityHotelRequest a AmenityHotel Entity El ID se setea como null automáticamente
   * para nuevas entidades
   *
   * @param request
   * @return
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true) // El hotel se debe setear manualmente
  AmenityHotel toEntity(AmenityHotelRequest request);

  /**
   * Convierte AmenityHotel Entity a AmenityHotelResponse
   *
   * @param amenityHotel
   * @return
   */
  @Mapping(target = "idHotel", source = "hotel.id")
  AmenityHotelResponse toResponse(AmenityHotel amenityHotel);

  /**
   * Actualiza una entidad existente con datos del request. Utilizado para operaciones de UPDATE
   *
   * @param request
   * @param amenityHotel
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true) // El hotel no se actualiza desde el request
  void updateEntityFromRequest(
      AmenityHotelRequest request, @MappingTarget AmenityHotel amenityHotel);
}
