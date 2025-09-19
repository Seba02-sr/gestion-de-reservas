package edu.utn.frsf.isi.dan.gestion.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HotelMapper {

    /**
     * Convierte HotelRequest a Hotel Entity
     * El ID se setea como null automaticamente para nuevas entidades
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
     * @param hotel
     * @return
     */
    HotelResponse toResponse(Hotel hotel);

    /**
     * Actualiza untidad existente con datos del request.
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param hotel
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(HotelRequest request, @MappingTarget Hotel hotel);
}