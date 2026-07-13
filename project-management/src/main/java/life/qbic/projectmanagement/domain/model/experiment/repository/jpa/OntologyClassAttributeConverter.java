package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import tools.jackson.core.JsonProcessingException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

@Converter(autoApply = true)

public class OntologyClassAttributeConverter implements
    AttributeConverter<OntologyTerm, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper().configure(
      DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public String convertToDatabaseColumn(OntologyTerm attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OntologyTerm convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, OntologyTerm.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
