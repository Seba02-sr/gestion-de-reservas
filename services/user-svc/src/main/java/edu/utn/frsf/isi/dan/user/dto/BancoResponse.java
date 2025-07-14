package edu.utn.frsf.isi.dan.user.dto;

import edu.utn.frsf.isi.dan.user.model.Banco;

public record BancoResponse(
    Integer id,
    String nombre
) {
    public Banco toBanco() {
        return new Banco(id, nombre);
    }

    public static BancoResponse fromBanco(Banco banco) {
        return new BancoResponse(banco.getId(), banco.getNombre());
    }
}
