package edu.utn.frsf.isi.dan.gestion.dto;

import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import jakarta.validation.constraints.NotNull;

public record AmenityHotelRequest(
    @NotNull(message = "La amenidad no puede ser nula") Amenity amenity) {}
