package edu.utn.frsf.isi.dan.reservas_svc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sin Mongo ni RabbitMQ: no se crean índices y el listener no arranca
@SpringBootTest(
    properties = {
      "reservas.crear-indices=false",
      "spring.rabbitmq.listener.simple.auto-startup=false"
    })
class ReservasSvcApplicationTests {

  @Test
  void contextLoads() {}
}
