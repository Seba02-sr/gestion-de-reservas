package edu.utn.frsf.isi.dan.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TarjetaCreditoMapper {
    /**
     * Convierte TarjetaCreditoRequest a TarjetaCredito Entity
     * El ID se setea como null automaticamente para entidades nuevas 
     * @param request
     * @return
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "huesped", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    TarjetaCredito toEntity(TarjetaCreditoRequest request);
    
    /**
     * Convierte TarjetaCredito Entity a TarjetaCreditoResponse
     * @param tarjetaCredito
     * @return
     */
    @Mapping(target = "idBanco", source = "banco.id")
    TarjetaCreditoResponse toResponse(TarjetaCredito tarjetaCredito);
    

    /**
     * Actualiza una entidad existente con datos del request
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param tarjetaCredito
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "huesped", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    void updateEntityFromRequest(TarjetaCreditoRequest request, @MappingTarget TarjetaCredito tarjetaCredito);
}
