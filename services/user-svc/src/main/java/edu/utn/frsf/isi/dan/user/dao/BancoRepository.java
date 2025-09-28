package edu.utn.frsf.isi.dan.user.dao;

import edu.utn.frsf.isi.dan.user.model.Banco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BancoRepository extends JpaRepository<Banco, Integer> {
  public List<Banco> findByActivoTrueAndNombreContainingIgnoreCase(String nombre);

  List<Banco> findByActivoTrue();

  Optional<Banco> findByIdAndActivoTrue(Integer id);
}
