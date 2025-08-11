package edu.utn.frsf.isi.dan.user.service;

import edu.utn.frsf.isi.dan.user.dao.UsuarioRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.mapper.PropietarioMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.Propietario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HuespedMapper huespedMapper;

    @Autowired
    private PropietarioMapper propietarioMapper;

    /**
     * El mapper ya se encarga de:
     * 1- Setear el banco a cada tarjeta de credito
     * 2- Setear la tarjeta de credito al huesped
     * 
     * Asegurarse de tener cada Tarjeta de Credito seteado el huesped antes de
     * guardar
     * 
     * @param huespedRequest
     * @return
     */
    public Huesped crearUsuarioHuesped(HuespedRequest huespedRequest) {
        if (huespedRequest == null || huespedRequest.tarjetasCredito() == null
                || huespedRequest.tarjetasCredito().isEmpty()) {
            throw new IllegalArgumentException("El request o la lista de tarjetas no puede ser nula o vacía");
        }

        Huesped usuario = huespedMapper.toEntity(huespedRequest);

        // Relacion inversa, setear huesped a tarjeta de credito
        if (usuario.getTarjetaCredito() != null) {
            usuario.getTarjetaCredito().forEach(t -> t.setHuesped(usuario));
        }
        return usuarioRepository.save(usuario);
    }

    /**
     * El mapper ya se encarga de:
     * 1- Setear la cuenta bancaria al propietario
     * 
     * Asegurarse de tener una cuenta bancaria seteado al propietario antes de
     * guardar
     * 
     * @param propietarioRequest
     * @return
     */
    public Propietario crearUsuarioPropietario(PropietarioRequest propietarioRequest) {
        if (propietarioRequest == null || propietarioRequest.cuentaBancaria() == null) {
            throw new IllegalArgumentException("El request o la cuenta vancaria no puede ser nula");
        }

        Propietario usuario = propietarioMapper.toEntity(propietarioRequest);

        // Relacion inversa, setear propietario a cuenta bancaria
        if (usuario.getCuentaBancaria() != null) {
            usuario.getCuentaBancaria().setPropietario(usuario);
        }

        return usuarioRepository.save(usuario);
    }

}