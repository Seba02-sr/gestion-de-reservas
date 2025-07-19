package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

public record PropietarioResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    String username,
    Boolean activo,
    LocalDateTime fechaRegistro,
    LocalDateTime fechaEliminado,
    Long idHotel,
    CuentaResponse cuentaBancaria
) {}
