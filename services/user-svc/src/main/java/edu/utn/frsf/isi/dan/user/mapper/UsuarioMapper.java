package edu.utn.frsf.isi.dan.user.mapper;

import edu.utn.frsf.isi.dan.user.dto.UsuarioResponse;
import edu.utn.frsf.isi.dan.user.mapper.config.MapstructConfig;
import edu.utn.frsf.isi.dan.user.model.Usuario;
import org.mapstruct.Mapper;

@Mapper(config = MapstructConfig.class)
public interface UsuarioMapper {
  UsuarioResponse toResponse(Usuario usuario);
}
