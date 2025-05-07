package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.OntologyTermV1;

@Converter(autoApply = true)

public class OntologyClassAttributeConverter implements
    AttributeConverter<OntologyTermV1, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public String convertToDatabaseColumn(OntologyTermV1 attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OntologyTermV1 convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, OntologyTermV1.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
