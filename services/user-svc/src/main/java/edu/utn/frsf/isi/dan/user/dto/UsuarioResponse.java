package edu.utn.frsf.isi.dan.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record UsuarioResponse(
    Integer id,
    String nombre,
    String email,
    String telefono,
    String dni,
    String username,
    Boolean activo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaRegistro,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaEliminado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaModificado) {}
