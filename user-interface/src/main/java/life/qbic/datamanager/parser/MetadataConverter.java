package life.qbic.datamanager.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementProperty;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MetadataConverter implements MeasurementMetadataConverter {

  private MetadataConverter() {
  }

  public static MetadataConverter create() {
    return new MetadataConverter();
  }

  @Override
  public List<MeasurementMetadata> convert(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, IllegalMetadataException, MissingMetadataPropertyException {
    parsingResult.keys().keySet().forEach(key -> {
      System.out.printf("%s has matching property(true/false): %s%n", key,
          NGSMeasurementProperty.fromString(key).isPresent());
    });
    parsingResult.keys().keySet().forEach(key -> {
      System.out.printf("%s has matching property(true/false): %s%n", key,
          ProteomicsMeasurementProperty.fromString(key).isPresent());
    });
    return List.of();
  }


  enum ProteomicsMeasurementProperty {
    MEASUREMENT_ID("measurement id"),
    ORGANISATION_ID("organisation id");

    private final String name;

    ProteomicsMeasurementProperty(String value) {
      this.name = value;
    }

    static String trimValue(String value) {
      return value.trim().toLowerCase();
    }

    static Optional<ProteomicsMeasurementProperty> fromString(String value) {
      var trimmed = trimValue(value);
      return Arrays.stream(ProteomicsMeasurementProperty.values())
          .filter(property -> property.propertyName().equals(trimmed)).findFirst();
    }

    static boolean valueMatchesAnyProperty(String value) {
      var trimmedValue = trimValue(value);
      return Arrays.stream(MeasurementProperty.values()).map(MeasurementProperty::name)
          .anyMatch(trimmedValue::equalsIgnoreCase);
    }

    public String propertyName() {
      return name;
    }

  }

  enum NGSMeasurementProperty {
    MEASUREMENT_ID("measurement id"),
    ORGANISATION_ID("organisation id");

    private final String name;

    NGSMeasurementProperty(String value) {
      this.name = value;
    }

    static String trimValue(String value) {
      return value.trim().toLowerCase();
    }

    static Optional<NGSMeasurementProperty> fromString(String value) {
      var trimmed = trimValue(value);
      return Arrays.stream(NGSMeasurementProperty.values())
          .filter(property -> property.propertyName().equals(trimmed)).findFirst();
    }

    static boolean valueMatchesAnyProperty(String value) {
      var trimmedValue = trimValue(value);
      return Arrays.stream(MeasurementProperty.values()).map(MeasurementProperty::name)
          .anyMatch(trimmedValue::equalsIgnoreCase);
    }

    String propertyName() {
      return name;
    }
  }

  static class NGSMeasurementConverter {


    List<NGSMeasurementMetadata> convert(ParsingResult parsingResult) {
      List<NGSMeasurementMetadata> measurements = new ArrayList<>();
      return measurements;
    }


  }

}
