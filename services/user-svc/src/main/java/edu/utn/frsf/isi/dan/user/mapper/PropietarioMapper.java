package edu.utn.frsf.isi.dan.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.model.Propietario;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = CuentaMapper.class)
public interface PropietarioMapper {

    /**
     * Convierte PropietarioRequest a Propietario Entity
     * El ID se setea como null automaticamente para nuevas entidades
     * @param request
     * @return
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaEliminado", ignore = true)
    Propietario toEntity(PropietarioRequest request);

    /**
     * Convierte Propietario Entity a PropietarioResponse
     * @param propietario
     * @return
     */
    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    PropietarioResponse toResponse(Propietario propietario);

    /**
     * Actualiza una entidad existente con datos del request
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param propietario
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaEliminado", ignore = true)
    void updateEntityFromRequest(PropietarioRequest request, @MappingTarget Propietario propietario);
}
