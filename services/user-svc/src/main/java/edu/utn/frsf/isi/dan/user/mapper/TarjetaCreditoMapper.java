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
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "huesped", ignore = true)
    TarjetaCredito toEntity(TarjetaCreditoRequest request);
    
    @Mapping(target = "idBanco", source = "banco.id")
    TarjetaCreditoResponse toResponse(TarjetaCredito tarjetaCredito);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "huesped", ignore = true)
    void updateEntityFromRequest(TarjetaCreditoRequest request, @MappingTarget TarjetaCredito tarjetaCredito);
}
