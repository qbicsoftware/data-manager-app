package life.qbic.projectmanagement.domain.project.experiment.repository.jpa;

import java.util.Objects;
import javax.persistence.AttributeConverter;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;

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
