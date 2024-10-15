package life.qbic.datamanager.export.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.qbic.datamanager.export.model.isa.Investigation;

/**
 * <b>JSON serialization implementation of the {@link Serializer} interface</b>
 *
 * @since 1.5.0
 */
public class JsonSerializer implements Serializer {

  @Override
  public String serialize(Investigation investigation) throws SerializationException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writer().withDefaultPrettyPrinter();
    try {
      return mapper.writeValueAsString(investigation);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Serialization failed", e);
    }
  }
}
