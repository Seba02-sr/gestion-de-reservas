package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.dao.TarjetaCreditoRepository;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapperImpl;
import edu.utn.frsf.isi.dan.user.mapper.TarjetaCreditoMapperImpl;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Verifica contra una base real (H2) que borrar un huésped elimina sus tarjetas solo con el cascade
 * de JPA: el esquema lo genera Hibernate, sin ON DELETE CASCADE en la FK.
 */
@DataJpaTest(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
    })
@Import({HuespedService.class, HuespedMapperImpl.class, TarjetaCreditoMapperImpl.class})
class HuespedBorradoCascadaTest {

  @Autowired private HuespedService huespedService;
  @Autowired private HuespedRepository huespedRepository;
  @Autowired private TarjetaCreditoRepository tarjetaCreditoRepository;
  @Autowired private EntityManager em;

  private Huesped guardarHuespedConTarjetas(String dni) {
    Huesped huesped =
        Huesped.builder().nombre("Juan").dni(dni).tarjetaCredito(new ArrayList<>()).build();
    for (int i = 0; i < 2; i++) {
      TarjetaCredito t =
          TarjetaCredito.builder().numero("411111111111111" + i).esPrincipal(i == 0).build();
      t.setHuesped(huesped);
      huesped.getTarjetaCredito().add(t);
    }
    Huesped guardado = huespedRepository.saveAndFlush(huesped);
    em.clear();
    return guardado;
  }

  @Test
  void eliminarHuesped_BorraFisicamenteYEnCascadaSusTarjetas() {
    Huesped h1 = guardarHuespedConTarjetas("1111");
    Huesped h2 = guardarHuespedConTarjetas("2222");
    assertEquals(4, tarjetaCreditoRepository.count());

    huespedService.eliminarHuesped(h1.getId());
    em.flush();
    em.clear();

    assertFalse(huespedRepository.existsById(h1.getId()));
    assertTrue(tarjetaCreditoRepository.findByHuespedId(h1.getId()).isEmpty());
    // el otro huésped y sus tarjetas siguen intactos
    assertTrue(huespedRepository.existsById(h2.getId()));
    assertEquals(2, tarjetaCreditoRepository.findByHuespedId(h2.getId()).size());
    assertEquals(2, tarjetaCreditoRepository.count());
  }

  @Test
  void eliminarHuesped_YaNoApareceEnConsultas() {
    Huesped h = guardarHuespedConTarjetas("3333");

    huespedService.eliminarHuesped(h.getId());
    em.flush();
    em.clear();

    List<Huesped> todos = huespedRepository.findAll();
    assertTrue(todos.isEmpty());
    assertThrows(EntityNotFoundException.class, () -> huespedService.getHuespedById(h.getId()));
  }
}
