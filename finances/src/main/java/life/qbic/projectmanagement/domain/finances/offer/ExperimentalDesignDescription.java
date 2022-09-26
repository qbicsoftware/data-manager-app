package life.qbic.projectmanagement.domain.finances.offer;

import java.util.Objects;
import javax.persistence.AttributeConverter;

/**
 * Experimental design description in an offer
 *
 * @param description the description
 * @since 1.0.0
 */
public record ExperimentalDesignDescription(String description) {

  public static ExperimentalDesignDescription from(String description) {
    return new ExperimentalDesignDescription(description);
  }

  public static class Converter implements
      AttributeConverter<ExperimentalDesignDescription, String> {

    @Override
    public String convertToDatabaseColumn(
        ExperimentalDesignDescription experimentalDesignDescription) {
      if (Objects.isNull(experimentalDesignDescription)) {
        return "";
      }
      return experimentalDesignDescription.description();
    }

    @Override
    public ExperimentalDesignDescription convertToEntityAttribute(String dbData) {
      if (Objects.isNull(dbData) || dbData.isEmpty()) {
        return null;
      }
      return ExperimentalDesignDescription.from(dbData);
    }
  }
}
