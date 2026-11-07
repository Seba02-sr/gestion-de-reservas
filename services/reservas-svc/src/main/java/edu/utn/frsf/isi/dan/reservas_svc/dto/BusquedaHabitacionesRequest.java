package edu.utn.frsf.isi.dan.reservas_svc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filtros (todos opcionales y combinables con AND) de la búsqueda de habitaciones disponibles.
 * {@code checkIn}/{@code checkOut} van juntos; {@code lat}/{@code lng}/{@code radioKm} van juntos.
 * {@code hotelId} limita la búsqueda a las habitaciones de un hotel.
 */
@Builder
public record BusquedaHabitacionesRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
    @Positive(message = "cantidadHuespedes debe ser mayor a 0") Integer cantidadHuespedes,
    @PositiveOrZero(message = "precioMin no puede ser negativo") Double precioMin,
    @PositiveOrZero(message = "precioMax no puede ser negativo") Double precioMax,
    @PositiveOrZero(message = "estrellas no puede ser negativo") Integer estrellas,
    List<String> amenities,
    @DecimalMin(value = "-90.0", message = "lat debe estar entre -90 y 90")
        @DecimalMax(value = "90.0", message = "lat debe estar entre -90 y 90")
        Double lat,
    @DecimalMin(value = "-180.0", message = "lng debe estar entre -180 y 180")
        @DecimalMax(value = "180.0", message = "lng debe estar entre -180 y 180")
        Double lng,
    @Positive(message = "radioKm debe ser mayor a 0") Double radioKm,
    @Positive(message = "hotelId debe ser mayor a 0") Integer hotelId) {}
