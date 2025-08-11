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

    /**
     * Convierte CuentaBancariaRequest a CuentaBancaria Entity
     * El ID se setea como null automaticamente para nuevas entidades
     * @param request
     * @return
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "propietario", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    CuentaBancaria toEntity(CuentaRequest request);

    /**
     * Convierte CuentaBancaria entity a CuentaBancariaResponse
     * @param cuentaBancaria
     * @return
     */
    @Mapping(target = "idBanco", source = "banco.id")
    CuentaResponse toResponse(CuentaBancaria cuentaBancaria);

    /**
     * Actualiza una entidad existente con datos del request
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param cuentaBancaria
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banco.id", source = "idBanco")
    @Mapping(target = "banco.nombre", ignore = true)
    @Mapping(target = "propietario", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    void updateEntityFromRequest(CuentaRequest request, @MappingTarget CuentaBancaria cuentaBancaria);
}
