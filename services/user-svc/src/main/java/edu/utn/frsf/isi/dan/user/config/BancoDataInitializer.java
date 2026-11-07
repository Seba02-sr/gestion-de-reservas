package edu.utn.frsf.isi.dan.user.config;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.model.Banco;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga inicial de bancos. El compose no monta infra/mysql/initdb (el esquema lo crea Hibernate),
 * así que en una base nueva no habría bancos y el registro de huéspedes (que exige una tarjeta con
 * banco) quedaría bloqueado. Es idempotente: solo inserta si la tabla está vacía.
 */
@Component
@Order(0)
@Slf4j
public class BancoDataInitializer implements ApplicationRunner {

  static final List<String> BANCOS =
      List.of(
          "Banco de la Nación Argentina",
          "Banco de la Provincia de Buenos Aires",
          "Banco Santander",
          "Banco Galicia",
          "BBVA",
          "Banco Macro",
          "Banco Credicoop",
          "HSBC",
          "ICBC",
          "Banco Ciudad",
          "Banco Patagonia",
          "Banco Supervielle",
          "Banco Itaú",
          "Banco Hipotecario",
          "Banco de Santa Fe");

  private final BancoRepository bancoRepository;

  public BancoDataInitializer(BancoRepository bancoRepository) {
    this.bancoRepository = bancoRepository;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (bancoRepository.count() > 0) {
      return;
    }
    bancoRepository.saveAll(BANCOS.stream().map(n -> Banco.builder().nombre(n).build()).toList());
    log.info("Bancos iniciales cargados: {}", BANCOS.size());
  }
}
