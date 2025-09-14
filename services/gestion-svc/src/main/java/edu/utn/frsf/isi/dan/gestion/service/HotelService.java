package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.AmenityHotelMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
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

    @Autowired
    private AmenityHotelMapper amenityHotelMapper;

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

    /**
     * Agrega uno o más amenities a un hotel existente.
     * 
     * @param id Identificador del hotel al que se agregarán los amenities.
     * @param amenityRequests Lista de AmenityHotelRequest a agregar.
     * @return HotelResponse con los datos actualizados.
     */
    @Transactional
    public HotelResponse agregarAmenities(Integer id, List<AmenityHotelRequest> amenityRequests) {
        if (amenityRequests == null || amenityRequests.isEmpty()) {
            log.warn("Se intentó agregar amenities con una lista nula o vacía");
            throw new IllegalArgumentException("La lista de amenities no puede ser nula o vacía");
        }

        log.info("Iniciando la adición de amenities al hotel con ID {}", id);

        Optional<Hotel> optionalHotel = hotelRepository.findById(id);
        if (optionalHotel.isEmpty()) {
            log.warn("No se encontró un hotel con ID {}", id);
            throw new IllegalArgumentException("El hotel con el ID especificado no existe");
        }

        try {
            Hotel hotelExistente = optionalHotel.get();
            List<AmenityHotel> amenityHotels = hotelExistente.getAmenities();

            for (AmenityHotelRequest request : amenityRequests) {
                AmenityHotel amenityHotel = amenityHotelMapper.toEntity(request);
                amenityHotel.setHotel(hotelExistente);
                amenityHotels.add(amenityHotel);
            }

            hotelExistente.setAmenities(amenityHotels);
            Hotel hotelActualizado = hotelRepository.save(hotelExistente);
            return hotelMapper.toResponse(hotelActualizado);
        } catch (Exception e) {
            log.error("Error al agregar amenities al hotel: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Elimina un amenity de un hotel existente.
     * 
     * @param id Identificador del hotel.
     * @param amenityId Identificador del amenity a eliminar.
     */
    @Transactional
    public void eliminarAmenity(Integer id, Long amenityId) {
        if (id == null || amenityId == null) {
            log.warn("Se intentó eliminar un amenity con id o amenityId nulos");
            throw new IllegalArgumentException("El id del hotel y el id del amenity no pueden ser nulos");
        }

        log.info("Iniciando eliminación del amenity con ID {} del hotel con ID {}", amenityId, id);

        try {
            Optional<Hotel> optionalHotel = hotelRepository.findById(id);
            if (optionalHotel.isEmpty()) {
                log.warn("No se encontró un hotel con ID {}", id);
                throw new IllegalArgumentException("El hotel con el ID especificado no existe");
            }

            Hotel hotelExistente = optionalHotel.get();
            List<AmenityHotel> amenityHotels = hotelExistente.getAmenities();

            boolean removed = amenityHotels.removeIf(amenityHotel -> amenityHotel.getId().equals(amenityId));
            if (!removed) {
                log.warn("No se encontró un amenity con ID {} en el hotel con ID {}", amenityId, id);
                throw new IllegalArgumentException("El amenity con el ID especificado no existe en el hotel");
            }

            hotelExistente.setAmenities(amenityHotels);
            hotelRepository.save(hotelExistente);
            log.info("Amenity con ID {} eliminado del hotel con ID {}", amenityId, id);
        } catch (Exception e) {
            log.error("Error al eliminar el amenity: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Marca un hotel como cerrado.
     * 
     * @param id Identificador del hotel a cerrar.
     * @return HotelResponse con los datos actualizados.
     */
    @Transactional
    public HotelResponse cerrarHotel(Integer id) {
        if (id == null) {
            log.warn("Se intentó cerrar un hotel con un ID nulo");
            throw new IllegalArgumentException("El ID del hotel no puede ser nulo");
        }

        log.info("Iniciando el cierre del hotel con ID {}", id);

        try {
            Optional<Hotel> optionalHotel = hotelRepository.findById(id);
            if (optionalHotel.isEmpty()) {
                log.warn("No se encontró un hotel con ID {}", id);
                throw new IllegalArgumentException("El hotel con el ID especificado no existe");
            }

            Hotel hotelExistente = optionalHotel.get();
            hotelExistente.setCerrado(true);

            Hotel hotelActualizado = hotelRepository.save(hotelExistente);
            log.info("Hotel con ID {} marcado como cerrado", id);
            return hotelMapper.toResponse(hotelActualizado);
        } catch (Exception e) {
            log.error("Error al cerrar el hotel: {}", e.getMessage(), e);
            throw e;
        }
    }
}
