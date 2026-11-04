package edu.utn.frsf.isi.dan.gestion.dao;

import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HabitacionRepository
    extends JpaRepository<Habitacion, Integer>, JpaSpecificationExecutor<Habitacion> {

  List<Habitacion> findByHotelId(Integer hotelId);

  boolean existsByHotelIdAndNumero(Integer hotelId, Integer numero);

  boolean existsByHotelIdAndNumeroAndIdNot(Integer hotelId, Integer numero, Integer id);
}
