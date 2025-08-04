package edu.utn.frsf.isi.dan.user.util;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@MappedSuperclass
@Data
public abstract class AuditableEntity {

    @Column(nullable = false)
    protected Boolean activo = true;

    @Column(name = "fecha_eliminado")
    protected LocalDateTime fechaEliminado;

    @Column(name = "fecha_registro")
    protected LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    protected LocalDateTime fechaModificacion;

    public void marcarComoEliminado() {
        this.activo = false;
        this.fechaEliminado = LocalDateTime.now();
    }

    public void marcarComoModificado() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
