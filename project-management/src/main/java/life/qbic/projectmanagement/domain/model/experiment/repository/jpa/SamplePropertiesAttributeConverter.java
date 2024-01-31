package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.HashMap;

@Converter()
public class SamplePropertiesAttributeConverter implements AttributeConverter<HashMap<String, String>,
    String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(HashMap<String, String> attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public HashMap<String, String> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) {
        return new HashMap<>();
      } else {
        return objectMapper.readValue(dbData, HashMap.class);
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
