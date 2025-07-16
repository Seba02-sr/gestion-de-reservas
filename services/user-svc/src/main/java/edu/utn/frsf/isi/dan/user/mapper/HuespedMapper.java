package edu.utn.frsf.isi.dan.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.model.Huesped;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses= {TarjetaCreditoMapper.class})
public interface HuespedMapper {

    /**
     * Convierte HuespedRequest a Huesped Entity
     * El ID se setea como null automaticamente para nuevas entidades
     * 
     * @param request
     * @return
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tarjetaCredito", source="tarjetasCredito")
    Huesped toEntity(HuespedRequest request);

    /**
     * Convierte Huesped Entity a HuespedResponse
     * @param huesped
     * @return
     */
    HuespedResponse toResponse(Huesped huesped);

    /**
     * Actualiza untidad existente con datos del request.
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param huesped
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tarjetaCredito", source="tarjetasCredito")
    void updateEntityFromRequest(HuespedRequest request, @org.mapstruct.MappingTarget Huesped huesped);
}
