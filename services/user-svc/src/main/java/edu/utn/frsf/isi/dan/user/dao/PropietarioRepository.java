package edu.utn.frsf.isi.dan.user.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.utn.frsf.isi.dan.user.model.Propietario;

@Repository
public interface PropietarioRepository extends JpaRepository<Propietario, Long> {

    List<Propietario> findByNombreContainingIgnoreCase(String nombre);

    Optional<Propietario> findByDni(String dni);

    List<Propietario> findByDniStartingWith(String dni);
}
