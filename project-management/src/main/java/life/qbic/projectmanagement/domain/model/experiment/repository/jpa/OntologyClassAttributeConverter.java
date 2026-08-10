package life.qbic.projectmanagement.domain.model.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

@Converter(autoApply = true)
public class OntologyClassAttributeConverter implements
    AttributeConverter<OntologyTerm, String> {

  private final ObjectMapper objectMapper;

  public OntologyClassAttributeConverter(@NonNull ObjectMapper mapper) {
    objectMapper = mapper;
  }

  @Override

  public String convertToDatabaseColumn(OntologyTerm attribute) {
    return objectMapper.writeValueAsString(attribute);
  }

  @Override
  public OntologyTerm convertToEntityAttribute(String dbData) {
    return objectMapper.readValue(dbData, OntologyTerm.class);
  }

}
