package edu.utn.frsf.isi.dan.gestion.messaging;

import edu.utn.frsf.isi.dan.shared.HabitacionEvent;

/**
 * Evento de aplicación (Spring) que pide enviar un {@link HabitacionEvent} a reservas-svc. Se
 * publica dentro de la transacción y {@link HabitacionEventPublisher} lo envía a RabbitMQ recién
 * después del commit.
 */
public record HabitacionEventoPublicado(HabitacionEvent evento) {}
