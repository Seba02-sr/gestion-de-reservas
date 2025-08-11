package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

public record BancoResponse(
    Integer id,
    String nombre,
    Boolean activo,
    LocalDateTime fechaRegistro,
    LocalDateTime fechaEliminado
) {}
