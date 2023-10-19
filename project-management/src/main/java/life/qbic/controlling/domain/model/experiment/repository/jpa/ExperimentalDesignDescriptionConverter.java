package life.qbic.controlling.domain.model.experiment.repository.jpa;

import jakarta.persistence.AttributeConverter;
import java.util.Objects;
import life.qbic.controlling.domain.model.project.ExperimentalDesignDescription;

public class ExperimentalDesignDescriptionConverter implements
    AttributeConverter<ExperimentalDesignDescription, String> {


  @Override
  public String convertToDatabaseColumn(ExperimentalDesignDescription attribute) {
    if (Objects.isNull(attribute)) {
      return null;
    }
    return attribute.value();
  }

  @Override
  public ExperimentalDesignDescription convertToEntityAttribute(String dbData) {
    if (Objects.isNull(dbData)) {
      return null;
    }
    return ExperimentalDesignDescription.create(dbData);
  }
}
