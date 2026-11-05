package edu.utn.frsf.isi.dan.gestion.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a RabbitMQ (exchange topic {@code dan.exchange}) los eventos de habitaciones, solo después
 * de que la transacción que los originó hizo commit: si hay rollback no se notifica nada.
 *
 * <p>La routing key es {@code <prefijo>.<accion>} (p. ej. {@code dan.habitacion.crear}), que
 * coincide con el binding {@code dan.habitacion.#} de la cola {@code habitacion.topic}.
 */
@Component
@Log4j2
public class HabitacionEventPublisher {

  @Autowired private RabbitTemplate rabbitTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Value("${rabbitmq.exchange:dan.exchange}")
  private String exchange;

  @Value("${rabbitmq.routingkey.prefix:dan.habitacion}")
  private String routingKeyPrefix;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publicar(HabitacionEventoPublicado publicado) {
    HabitacionEvent evento = publicado.evento();
    try {
      String mensaje = objectMapper.writeValueAsString(evento);
      String routingKey = routingKey(evento.getTipoEvento());
      log.debug("[RabbitMQ] Enviando a {} / {}: {}", exchange, routingKey, mensaje);
      rabbitTemplate.convertAndSend(exchange, routingKey, mensaje);
    } catch (Exception e) {
      // La transacción ya se confirmó: no se puede deshacer; se deja registro del fallo.
      log.error("[RabbitMQ] No se pudo enviar el evento {}: {}", evento, e.getMessage(), e);
    }
  }

  String routingKey(TipoEvento tipo) {
    String accion =
        switch (tipo) {
          case CREAR -> "crear";
          case ACTUALIZAR_DATOS -> "actualizar";
          case ACTUALIZAR_PRECIO -> "precio";
          case ELIMINAR -> "eliminar";
          case CERRAR_HOTEL -> "cerrar-hotel";
        };
    return routingKeyPrefix + "." + accion;
  }
}
