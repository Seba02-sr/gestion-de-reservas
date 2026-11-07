package edu.utn.frsf.isi.dan.user.dao;

import edu.utn.frsf.isi.dan.user.model.Propietario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropietarioRepository extends JpaRepository<Propietario, Integer> {

  // Solo activos
  List<Propietario> findByActivoTrue();

  Optional<Propietario> findByIdAndActivoTrue(Integer id);

  List<Propietario> findByActivoTrueAndNombreContainingIgnoreCase(String nombre);

  Optional<Propietario> findByActivoTrueAndDni(String dni);

  List<Propietario> findByActivoTrueAndDniStartingWith(String dni);
}
