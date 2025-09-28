package edu.utn.frsf.isi.dan.user.dao;

import edu.utn.frsf.isi.dan.user.model.Huesped;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HuespedRepository extends JpaRepository<Huesped, Long> {

  // Búsquedas considerando solo usuarios activos
  List<Huesped> findByActivoTrue();

  Optional<Huesped> findByIdAndActivoTrue(Long id);

  List<Huesped> findByActivoTrueAndNombreContainingIgnoreCase(String nombre);

  Optional<Huesped> findByActivoTrueAndDni(String dni);
}
