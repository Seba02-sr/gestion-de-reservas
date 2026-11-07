package edu.utn.frsf.isi.dan.reservas_svc.testutil;

import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Pasa un {@link Query} / {@link Criteria} por el mismo mapeo que usa MongoTemplate (Instant →
 * Date, enums, objetos geo) para poder inspeccionar el documento que realmente se enviaría a Mongo,
 * sin necesidad de una base.
 */
public final class MongoQueries {

  private MongoQueries() {}

  private static final MongoMappingContext CONTEXTO;
  private static final QueryMapper MAPPER;

  static {
    MongoCustomConversions conversiones = new MongoCustomConversions(List.of());
    CONTEXTO = new MongoMappingContext();
    CONTEXTO.setSimpleTypeHolder(conversiones.getSimpleTypeHolder());
    CONTEXTO.afterPropertiesSet();
    MappingMongoConverter converter =
        new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, CONTEXTO);
    converter.setCustomConversions(conversiones);
    converter.afterPropertiesSet();
    MAPPER = new QueryMapper(converter);
  }

  public static Document mapear(Query query) {
    return MAPPER.getMappedObject(
        query.getQueryObject(), CONTEXTO.getPersistentEntity(Habitacion.class));
  }

  public static Document mapear(Criteria criteria) {
    return mapear(new Query(criteria));
  }
}
