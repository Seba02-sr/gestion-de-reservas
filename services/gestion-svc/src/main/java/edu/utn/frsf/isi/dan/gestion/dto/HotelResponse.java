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
    Integer categoria,
    Boolean cerrado
    // Falta lista de amenities y habitaciones (agregar cuando se crees los dto de las mismas y sus
    // mapper)
    ) {}
