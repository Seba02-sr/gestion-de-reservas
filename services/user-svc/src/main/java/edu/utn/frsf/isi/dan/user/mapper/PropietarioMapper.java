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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    @Mapping(target = "idHotel", source = "idHotel")
    Propietario toEntity(PropietarioRequest request);

    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    @Mapping(target = "idHotel", source = "idHotel")
    PropietarioResponse toResponse(Propietario propietario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuentaBancaria", source = "cuentaBancaria")
    @Mapping(target = "idHotel", source = "idHotel")
    void updateEntityFromRequest(PropietarioRequest request, @MappingTarget Propietario propietario);
}
