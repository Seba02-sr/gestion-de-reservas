package edu.utn.frsf.isi.dan.gestion.dto;

import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import jakarta.validation.constraints.NotNull;

public record AmenityHotelRequest(
    @NotNull(message = "El id del hotel no puede ser nulo") Integer idHotel,
    @NotNull(message = "La amenidad no puede ser nula") Amenity amenity) {}
