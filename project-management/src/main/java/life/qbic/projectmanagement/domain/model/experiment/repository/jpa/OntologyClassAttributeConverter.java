package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;

@Converter(autoApply = true)

public class OntologyClassAttributeConverter implements
    AttributeConverter<OntologyClassDTO, String> {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(OntologyClassDTO attribute) {
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OntologyClassDTO convertToEntityAttribute(String dbData) {
    try {
      return objectMapper.readValue(dbData, OntologyClassDTO.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
