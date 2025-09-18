package edu.utn.frsf.isi.dan.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GestionSvcApplicationTests {

  @Test
  void contextLoads() {}

  // Evita conexiones reales a Rabbit durante el arranque del contexto
  @MockBean private RabbitTemplate rabbitTemplate;
}
