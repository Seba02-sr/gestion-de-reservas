package edu.utn.frsf.isi.dan.gestion.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  /** Cola que consume reservas-svc (debe coincidir con su @RabbitListener). */
  public static final String COLA_HABITACIONES = "habitacion.topic";

  /**
   * Declara en el broker (al abrir la primera conexión) el exchange, la cola y el binding. Sin
   * esto, un evento publicado antes de que reservas-svc haya arrancado alguna vez se perdería en
   * silencio (exchange o cola inexistentes). Los parámetros son los mismos que declara
   * reservas-svc, así que ambos servicios pueden declararlos sin conflicto.
   */
  @Bean
  public Declarables habitacionesTopologia(
      @Value("${rabbitmq.exchange:dan.exchange}") String exchange,
      @Value("${rabbitmq.routingkey.prefix:dan.habitacion}") String routingKeyPrefix) {
    TopicExchange topicExchange = new TopicExchange(exchange, true, false);
    Queue cola = new Queue(COLA_HABITACIONES, true);
    return new Declarables(
        topicExchange,
        cola,
        BindingBuilder.bind(cola).to(topicExchange).with(routingKeyPrefix + ".#"));
  }

  @Bean
  public MessageConverter jackson2MessageConverter() {
    // Este convertidor usará Jackson para serializar/deserializar objetos a/desde JSON.
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(messageConverter);
    // propaga el traceparent en los headers del mensaje: reservas-svc continúa la misma traza
    rabbitTemplate.setObservationEnabled(true);
    return rabbitTemplate;
  }
}
