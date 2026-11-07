package edu.utn.frsf.isi.dan.reservas_svc.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lo único que reservas-svc necesita saber de un huésped. Se ignora el resto del JSON de user-svc
 * (tarjetas, DNI, etc.) para no acoplarse a su modelo ni manejar datos sensibles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HuespedInfo(Integer id, String nombre, String email) {}
