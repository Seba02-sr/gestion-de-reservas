package edu.utn.frsf.isi.dan.user.dto;

public record PropietarioResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    Long idHotel,
    CuentaResponse cuentaBancaria
) {}
