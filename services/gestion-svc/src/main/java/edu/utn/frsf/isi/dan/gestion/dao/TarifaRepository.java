package edu.utn.frsf.isi.dan.gestion.dao;

import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Integer> {

  /**
   * Tarifas del tipo vigentes en {@code fecha} (fechaFin null = sin vencimiento), la más nueva
   * primero.
   */
  @Query(
      "select t from Tarifa t where t.tipoHabitacion.id = :tipoId and t.fechaInicio <= :fecha "
          + "and (t.fechaFin is null or t.fechaFin >= :fecha) order by t.fechaInicio desc")
  List<Tarifa> findVigentes(@Param("tipoId") Integer tipoId, @Param("fecha") LocalDate fecha);
}
