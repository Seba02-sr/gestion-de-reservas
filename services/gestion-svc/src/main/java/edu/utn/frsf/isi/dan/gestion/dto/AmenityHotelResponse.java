package edu.utn.frsf.isi.dan.gestion.dto;

import edu.utn.frsf.isi.dan.gestion.model.Amenity;

public record AmenityHotelResponse(
    Integer id,
    Integer idHotel,
    Amenity amenity
) {

}
