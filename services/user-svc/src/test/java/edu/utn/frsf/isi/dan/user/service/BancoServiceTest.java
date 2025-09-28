package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.mapper.BancoMapper;
import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BancoServiceTest {

  @Mock private BancoRepository repo;
  @Mock private BancoMapper mapper;
  @InjectMocks private BancoService service;

  private Banco entidad;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    entidad = Banco.builder().id(1).nombre("Bank").build();
  }

  @Test
  void getById_NotFound_Throws404() {
    when(repo.findByIdAndActivoTrue(1)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> service.getBancoById(1));
  }

  @Test
  void create_MapsAndSaves() {
    BancoRequest req = BancoRequest.builder().nombre("X").build();
    when(mapper.toEntity(req)).thenReturn(entidad);
    when(repo.save(entidad)).thenReturn(entidad);
    when(mapper.toResponse(entidad)).thenReturn(BancoResponse.builder().id(1).nombre("X").build());

    BancoResponse out = service.createBanco(req);
    assertEquals(1, out.id());
  }

  @Test
  void update_MarksModifiedAndSaves() {
    BancoRequest req = BancoRequest.builder().nombre("Y").build();
    when(repo.findByIdAndActivoTrue(1)).thenReturn(Optional.of(entidad));
    doAnswer(
            inv -> {
              entidad.setNombre("Y");
              return null;
            })
        .when(mapper)
        .updateEntityFromRequest(eq(req), any(Banco.class));
    when(repo.save(entidad)).thenReturn(entidad);
    when(mapper.toResponse(entidad)).thenReturn(BancoResponse.builder().id(1).nombre("Y").build());

    BancoResponse out = service.updateBanco(1, req);
    assertEquals("Y", out.nombre());
  }

  @Test
  void delete_SoftDelete() {
    when(repo.findByIdAndActivoTrue(1)).thenReturn(Optional.of(entidad));
    when(repo.save(any(Banco.class))).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(Banco.class))).thenReturn(BancoResponse.builder().id(1).build());
    BancoResponse out = service.deleteBanco(1);
    assertFalse(entidad.getActivo());
    assertEquals(1, out.id());
  }
}
