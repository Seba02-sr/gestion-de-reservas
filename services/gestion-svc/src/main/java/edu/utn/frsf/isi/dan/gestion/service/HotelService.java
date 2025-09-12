package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Log4j2
public class HotelService {
    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelMapper hotelMapper;

    /**
     * El mapper ya se encarga de:
     * 1- Inicializar habitaciones y amenities como listas vacías
     * 
     * @param hotelRequest
     * @return
     */
    @Transactional
    public HotelResponse crearHotel(HotelRequest hotelRequest) {
        if(hotelRequest == null) {
            log.warn("Se intentó crear un hotel con una solicitud nula");
            throw new IllegalArgumentException("La transaccion no puede ser nula");
        }

        log.info("Iniciando registro de un hotel con nombre {} y cuit {}", hotelRequest.nombre(), hotelRequest.cuit());
        
        try {
            Hotel hotelNuevo = hotelMapper.toEntity(hotelRequest);
            Hotel hotelGuardado = hotelRepository.save(hotelNuevo);
            return hotelMapper.toResponse(hotelGuardado);
        } catch (Exception e) {
            log.error("Error al crear el hotel: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Actualiza un hotel existente con los datos permitidos.
     * Solo se pueden actualizar la categoría, el teléfono y el correo de contacto.
     * 
     * @param id Identificador del hotel a actualizar.
     * @param hotelRequest Datos a actualizar.
     * @return HotelResponse con los datos actualizados.
     */
    @Transactional
    public HotelResponse actualizarHotel(Integer id, HotelRequest hotelRequest) {
        if (hotelRequest == null) {
            log.warn("Se intentó actualizar un hotel con una solicitud nula");
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }

        log.info("Iniciando actualización del hotel con ID {}", id);

        Optional<Hotel> optionalHotel = hotelRepository.findById(id);
        if (optionalHotel.isEmpty()) {
            log.warn("No se encontró un hotel con ID {}", id);
            throw new IllegalArgumentException("El hotel con el ID especificado no existe");
        }

        try {
            Hotel hotelExistente = optionalHotel.get();
            hotelExistente.setCategoria(hotelRequest.categoria());
            hotelExistente.setTelefono(hotelRequest.telefono());
            hotelExistente.setCorreoContacto(hotelRequest.correoContacto());

            Hotel hotelActualizado = hotelRepository.save(hotelExistente);
            return hotelMapper.toResponse(hotelActualizado);
        } catch (Exception e) {
            log.error("Error al actualizar el hotel: {}", e.getMessage(), e);
            throw e;
        }
    }
}
