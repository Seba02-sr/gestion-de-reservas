package edu.utn.frsf.isi.dan.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.model.Banco;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BancoMapper {

    /**
     * Convierte BancoRequest a Banco Entity
     * El ID se setea como null automaticamente para nuevas entidades
     * @param request
     * @return
     */
    @Mapping(target = "id", ignore = true)
    Banco toEntity(BancoRequest request);

    /**
     * Convierte Banco entity a BancoResponse
     * @param banco
     * @return
     */
    BancoResponse toResponse(Banco banco);

    /**
     * Actualiza una entidad existente con datos del request
     * Utilizado para operaciones de UPDATE
     * @param request
     * @param banco
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(BancoRequest request, @org.mapstruct.MappingTarget Banco banco);

}
