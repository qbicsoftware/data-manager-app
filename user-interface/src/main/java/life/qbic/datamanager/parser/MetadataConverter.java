package life.qbic.datamanager.parser;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementProperty;
import life.qbic.logging.api.Logger;
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

  private static final Logger log = logger(MetadataConverter.class);

  private MetadataConverter() {
  }

  public static MetadataConverter create() {
    return new MetadataConverter();
  }

  private static Map<String, Integer> countHits(Collection<String> source, Set<String> target) {
    Map<String, Integer> hits = new HashMap<>();
    for (String t : target) {
      hits.put(t, 0);
    }
    for (String s : source) {
      if (target.contains(s)) {
        var currentHit = hits.get(s);
        hits.put(s, ++currentHit);
      }
    }
    return hits;
  }

  @Override
  public List<MeasurementMetadata> convert(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, IllegalMetadataException, MissingMetadataPropertyException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.keys().keySet();
    if (looksLikeNgsMeasurement(properties)) {
      return convertNGSMeasurement(parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties)) {
      return convertProteomicsMeasurement(parsingResult);
    } else {
      throw new UnknownMetadataTypeException(
          "Unknown metadata type: cannot match properties to any known metadata type. Provided [%s]".formatted(
              String.join(", ", properties)));
    }
  }

  private List<MeasurementMetadata> convertProteomicsMeasurement(ParsingResult parsingResult) {
    throw new RuntimeException("not implemented yet");
  }

  private List<MeasurementMetadata> convertNGSMeasurement(ParsingResult parsingResult) {
    throw new RuntimeException("not implemented yet");
  }

  private boolean looksLikeNgsMeasurement(Collection<String> properties) {
    var confirmedProperties = 0;
    for (String property : properties) {
      if (NGSMeasurementProperty.fromString(property).isPresent()) {
        confirmedProperties++;
      }
    }
    return confirmedProperties == properties.size();
  }

  private boolean looksLikeProteomicsMeasurement(Collection<String> properties) {
    var formattedProperties = properties.stream().map(String::toLowerCase).toList();
    var hitMap = countHits(formattedProperties, Arrays.stream(ProteomicsMeasurementProperty.values())
        .map(ProteomicsMeasurementProperty::propertyName).collect(
            Collectors.toSet()));
    var missingProperties = new ArrayList<>();
    for (Entry<String, Integer> entry : hitMap.entrySet()) {
      if (entry.getValue() == 0) {
        missingProperties.add(entry.getKey());
      }
    }
    if (missingProperties.isEmpty()) {
      return true;
    } else {
      log.debug("Missing properties for proteomics measurement: %s".formatted(missingProperties));
    }
    return false;
  }

  enum ProteomicsMeasurementProperty {
    MEASUREMENT_ID("measurement id"),
    QBIC_SAMPLE_ID("qbic sample id"),
    SAMPLE_POOL_GROUP("sample pool group"),
    ORGANISATION_ID("organisation id"),
    FACILITY("facility"),
    INSTRUMENT("instrument"),
    CYCLE("cycle/fraction name"),
    DIGESTION_METHOD("digestion method"),
    DIGESTION_ENZYME("digestion enzyme"),
    ENRICHMENT_METHOD("enrichment method"),
    INJECTION_VOLUME("injection volume (ul)"),
    LC_COLUMN("lc column"),
    LCMS_METHOD("lcms method"),
    LABELING_TYPE("labeling type"),
    LABEL("label"),
    COMMENT("comment");

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
