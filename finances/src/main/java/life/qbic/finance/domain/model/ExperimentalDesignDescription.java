package life.qbic.finance.domain.model;

import jakarta.persistence.AttributeConverter;

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
      return experimentalDesignDescription.description();
    }

    @Override
    public ExperimentalDesignDescription convertToEntityAttribute(String dbData) {
      return ExperimentalDesignDescription.from(dbData);
    }
  }
}
