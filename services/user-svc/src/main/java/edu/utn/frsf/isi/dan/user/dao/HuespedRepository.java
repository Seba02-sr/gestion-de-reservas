package edu.utn.frsf.isi.dan.user.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.utn.frsf.isi.dan.user.model.Huesped;

@Repository
public interface HuespedRepository extends JpaRepository<Huesped, Long> {

    List<Huesped> findByNombreContainingIgnoreCase(String nombre);

    Optional<Huesped> findByDni(String dni);
}
