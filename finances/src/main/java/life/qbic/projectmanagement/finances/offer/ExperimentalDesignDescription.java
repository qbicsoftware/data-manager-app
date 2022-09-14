package life.qbic.projectmanagement.finances.offer;

import javax.persistence.AttributeConverter;

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
    public ExperimentalDesignDescription convertToEntityAttribute(String s) {
      return ExperimentalDesignDescription.from(s);
    }
  }

}
