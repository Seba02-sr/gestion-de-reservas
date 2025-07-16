package edu.utn.frsf.isi.dan.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import edu.utn.frsf.isi.dan.user.dto.CuentaRequest;
import edu.utn.frsf.isi.dan.user.dto.CuentaResponse;
import edu.utn.frsf.isi.dan.user.model.CuentaBancaria;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CuentaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "propietario", ignore = true)
    CuentaBancaria toEntity(CuentaRequest request);

    @Mapping(target = "idBanco", source = "banco.id")
    CuentaResponse toResponse(CuentaBancaria cuentaBancaria);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "propietario", ignore = true)
    void updateEntityFromRequest(CuentaRequest request, @MappingTarget CuentaBancaria cuentaBancaria);
}
