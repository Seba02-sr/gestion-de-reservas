package edu.utn.frsf.isi.dan.gestion.dto;

import lombok.Builder;

@Builder
public record HotelResponse(
    Integer id,
    String nombre,
    String cuit,
    String domicilio,
    Double latitud,
    Double longitud,
    String telefono,
    String correoContacto,
    Integer categoria
) {

}
