package edu.utn.frsf.isi.dan.gestion.dto;

import lombok.Builder;

@Builder
public record TipoHabitacionResponse(
    Integer id, String nombre, String descripcion, Integer capacidad) {}
