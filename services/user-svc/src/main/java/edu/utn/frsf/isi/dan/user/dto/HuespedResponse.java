package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDate;
import java.util.List;

public record HuespedResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    LocalDate fechaNacimiento,
    List<TarjetaCreditoResponse> tarjetaCredito
) {}