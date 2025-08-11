package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HuespedResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    String username,
    LocalDateTime fechaRegistro,
    LocalDateTime fechaEliminado,
    Boolean activo,
    LocalDate fechaNacimiento,
    List<TarjetaCreditoResponse> tarjetaCredito
) {}