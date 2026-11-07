package edu.utn.frsf.isi.dan.user.dao;

import edu.utn.frsf.isi.dan.user.model.Huesped;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HuespedRepository extends JpaRepository<Huesped, Integer> {

  // Los huéspedes se borran físicamente (con sus tarjetas por cascade JPA): no hay filtro "activo".
  List<Huesped> findByNombreContainingIgnoreCase(String nombre);

  Optional<Huesped> findByDni(String dni);
}
