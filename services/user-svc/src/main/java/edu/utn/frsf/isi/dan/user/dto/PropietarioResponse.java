package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record PropietarioResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    String username,
    Boolean activo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaRegistro,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaEliminado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaModificado,
    Long idHotel,
    CuentaResponse cuentaBancaria
) {}
