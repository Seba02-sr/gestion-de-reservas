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

import java.util.List;
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

    public void deleteById(Integer id) {
        hotelRepository.deleteById(id);
    }

    public Optional<Hotel> findById(Integer id) {
        return hotelRepository.findById(id);
    }

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }
}
