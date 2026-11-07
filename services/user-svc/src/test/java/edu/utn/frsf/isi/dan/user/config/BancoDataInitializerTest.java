package edu.utn.frsf.isi.dan.user.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.utn.frsf.isi.dan.user.dao.BancoRepository;
import edu.utn.frsf.isi.dan.user.model.Banco;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BancoDataInitializerTest {

  private final BancoRepository repo = mock(BancoRepository.class);
  private final BancoDataInitializer initializer = new BancoDataInitializer(repo);

  @Test
  @SuppressWarnings("unchecked")
  void tablaVacia_cargaLosBancos() {
    when(repo.count()).thenReturn(0L);
    initializer.run(null);
    ArgumentCaptor<List<Banco>> captor = ArgumentCaptor.forClass(List.class);
    verify(repo).saveAll(captor.capture());
    assertEquals(BancoDataInitializer.BANCOS.size(), captor.getValue().size());
    assertEquals(true, captor.getValue().get(0).getActivo());
  }

  @Test
  void tablaConDatos_noInsertaNada() {
    when(repo.count()).thenReturn(3L);
    initializer.run(null);
    verify(repo, never()).saveAll(anyList());
  }
}
