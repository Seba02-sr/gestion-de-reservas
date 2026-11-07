package edu.utn.frsf.isi.dan.user.util;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditableEntity {

  @Column(nullable = false)
  @Builder.Default
  protected Boolean activo = true;

  @Column(name = "fecha_eliminado")
  protected LocalDateTime fechaEliminado;

  @Column(name = "fecha_registro")
  @Builder.Default
  protected LocalDateTime fechaRegistro = LocalDateTime.now();

  @Column(name = "fecha_modificacion")
  protected LocalDateTime fechaModificado;

  public void marcarComoEliminado() {
    this.activo = false;
    this.fechaEliminado = LocalDateTime.now();
  }

  public void marcarComoModificado() {
    this.fechaModificado = LocalDateTime.now();
  }
}
