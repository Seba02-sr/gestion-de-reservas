package edu.utn.frsf.isi.dan.user.dto;

import edu.utn.frsf.isi.dan.user.model.Banco;

public record BancoGetRecord(
    Integer id,
    String nombre
) {
    public Banco toBanco() {
        return new Banco(id, nombre);
    }
}
