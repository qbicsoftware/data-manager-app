package life.qbic.datamanager.parser;

import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.COMMENT;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.CYCLE;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.DIGESTION_ENZYME;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.DIGESTION_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.ENRICHMENT_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.FACILITY;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.INJECTION_VOLUME;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LABEL;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LABELING_TYPE;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LCMS_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LC_COLUMN;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.MEASUREMENT_ID;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.MS_DEVICE;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.ORGANISATION_ID;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.QBIC_SAMPLE_ID;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.SAMPLE_POOL_GROUP;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.parser.ParsingResult.Row;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.Labeling;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Metadata Converter</b>
 *
 * <p>Enables clients to convert {@link ParsingResult} objects into lists of known metadata
 * properties.</p>
 * <p>
 * Currently supported metadata properties cover:
 *
 * <ul>
 *   <li>Proteomics Measurement {@link ProteomicsMeasurementProperty}</li>
 *   <li>NGS Measurement {@link NGSMeasurementProperty}</li>
 * </ul>
 *
 * @since 1.4.0
 */
public class MetadataConverter implements MeasurementMetadataConverter {

  private static final Logger log = logger(MetadataConverter.class);

  private MetadataConverter() {
  }

  public static MetadataConverter measurementConverter() {
    return new MetadataConverter();
  }

  /**
   * Generates a hit map, storing the number of matches of a defined set of String values (hit
   * values), in a target of interest collection of String values.
   * <p>
   * The resulting map will contain the number of occurrences of every value in the hit values
   * collection found in the target collection to investigate.
   *
   * @param target    the collection of interest to search in
   * @param hitValues a set of distinct values, that should be represented in the hit result map
   * @return a hit result map, containing the number of occurrences of every hit value in the target
   * String collection (0, if no target was found for a value).
   * @since 1.4.0
   */
  private static Map<String, Integer> countHits(Collection<String> target, Set<String> hitValues,
      String... ignoredProperties) {
    Map<String, Integer> hits = new HashMap<>();
    for (String t : hitValues) {
      hits.put(t, 0);
    }
    for (String s : target) {
      if (hitValues.contains(s)) {
        var currentHit = hits.get(s);
        hits.put(s, ++currentHit);
      }
    }
    for (String ignoredProperty : ignoredProperties) {
      if (hits.containsKey((ignoredProperty))) {
        hits.remove(ignoredProperty);
      }
    }
    return hits;
  }

  static String sanitizeValue(String value) {
    return value.trim().toLowerCase();
  }

  @Override
  public List<MeasurementMetadata> convert(ParsingResult parsingResult, boolean ignoreMeasurementId)
      throws UnknownMetadataTypeException, MissingSampleIdException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.keys().keySet();
    if (looksLikeNgsMeasurement(properties, ignoreMeasurementId)) {
      return tryConversion(this::convertNGSMeasurement, parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, ignoreMeasurementId)) {
      return tryConversion(this::convertProteomicsMeasurement, parsingResult);
    } else {
      throw new UnknownMetadataTypeException(
          "Unknown metadata type: cannot match properties to any known metadata type. Provided [%s]".formatted(
              String.join(", ", properties)));
    }
  }

  private List<MeasurementMetadata> tryConversion(
      Function<ParsingResult, List<MeasurementMetadata>> converter, ParsingResult parsingResult) {
    try {
      return converter.apply(parsingResult);
    } catch (IllegalArgumentException e) {
      throw new MissingSampleIdException("Missing sample ID metadata");
    }
  }

  private List<MeasurementMetadata> convertProteomicsMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();
    var keyIndices = parsingResult.keys();
    for (ParsingResult.Row row : parsingResult.rows()) {
      // we us -1 as default value if a property cannot be accessed, thus ending up in an empty String
      var pxpMetaDatum = new ProteomicsMeasurementMetadata(
          safeListAccess(row.values(), keyIndices.getOrDefault(MEASUREMENT_ID.propertyName(), -1),
              ""),
          SampleCode.create(
              safeListAccess(row.values(),
                  keyIndices.getOrDefault(QBIC_SAMPLE_ID.propertyName(), -1),
                  "")),
          safeListAccess(row.values(), keyIndices.getOrDefault(ORGANISATION_ID.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(MS_DEVICE.propertyName(), -1), ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(SAMPLE_POOL_GROUP.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(FACILITY.propertyName(), -1), ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(CYCLE.propertyName(), -1), ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(DIGESTION_ENZYME.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(DIGESTION_METHOD.propertyName(), -1),
              ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(ENRICHMENT_METHOD.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(INJECTION_VOLUME.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(LC_COLUMN.propertyName(), -1), ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(LCMS_METHOD.propertyName(), -1), ""),
          new Labeling(
              safeListAccess(row.values(),
                  keyIndices.getOrDefault(LABELING_TYPE.propertyName(), -1),
                  ""),
              safeListAccess(row.values(), keyIndices.getOrDefault(LABEL.propertyName(), -1), "")),
          safeListAccess(row.values(), keyIndices.getOrDefault(COMMENT.propertyName(), -1), "")
      );
      result.add(pxpMetaDatum);
    }
    return result;
  }

  private String safeListAccess(List<String> list, Integer index, String defaultValue) {
    if (index >= list.size() || index < 0) {
      return defaultValue;
    }
    return list.get(index);
  }

  private List<MeasurementMetadata> convertNGSMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();
    var keyIndices = parsingResult.keys();
    for (Row row : parsingResult.rows()) {
      var ngsMeasurementMetadata = new NGSMeasurementMetadata(
          safeListAccess(row.values(), keyIndices.getOrDefault(MEASUREMENT_ID.propertyName(), -1),
              ""),
          List.of(SampleCode.create(
              safeListAccess(row.values(),
                  keyIndices.getOrDefault(QBIC_SAMPLE_ID.propertyName(), -1),
                  ""))),
          safeListAccess(row.values(), keyIndices.getOrDefault(ORGANISATION_ID.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(MS_DEVICE.propertyName(), -1), ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(FACILITY.propertyName(), -1), ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(
              NGSMeasurementProperty.SEQUENCING_READ_TYPE.propertyName(), -1), ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(NGSMeasurementProperty.LIBRARY_KIT.propertyName(), -1), ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(NGSMeasurementProperty.FLOW_CELL.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(
                  NGSMeasurementProperty.SEQUENCING_RUN_PROTOCOL.propertyName(), -1),
              ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(SAMPLE_POOL_GROUP.propertyName(), -1),
              ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(NGSMeasurementProperty.INDEX_I7.propertyName(), -1),
              ""),
          safeListAccess(row.values(),
              keyIndices.getOrDefault(NGSMeasurementProperty.INDEX_I5.propertyName(), -1),
              ""),
          safeListAccess(row.values(), keyIndices.getOrDefault(COMMENT.propertyName(), -1), "")
      );
      result.add(ngsMeasurementMetadata);
    }
    return result;
  }

  private boolean looksLikeNgsMeasurement(Collection<String> properties, boolean ignoreID) {
    var formattedProperties = properties.stream().map(String::toLowerCase)
        .collect(Collectors.toList());
    Map<String, Integer> hitMap;
    if (ignoreID) {
      formattedProperties.remove(MEASUREMENT_ID.propertyName());
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementProperty.values())
              .map(NGSMeasurementProperty::propertyName).collect(
                  Collectors.toSet()), MEASUREMENT_ID.propertyName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementProperty.values())
              .map(NGSMeasurementProperty::propertyName).collect(
                  Collectors.toSet()));
    }
    var missingProperties = new ArrayList<>();
    for (Entry<String, Integer> entry : hitMap.entrySet()) {
      if (entry.getValue() == 0) {
        missingProperties.add(entry.getKey());
      }
    }
    if (missingProperties.isEmpty()) {
      return true;
    } else {
      log.debug("Missing properties for NGS measurement: %s".formatted(missingProperties));
    }
    return false;
  }

  private boolean looksLikeProteomicsMeasurement(Collection<String> properties, boolean ignoreID) {
    var formattedProperties = properties.stream().map(String::toLowerCase)
        .collect(Collectors.toList());
    Map<String, Integer> hitMap;
    if (ignoreID) {
      formattedProperties.remove(MEASUREMENT_ID.propertyName());
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementProperty.values())
              .map(ProteomicsMeasurementProperty::propertyName).collect(
                  Collectors.toSet()), MEASUREMENT_ID.propertyName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementProperty.values())
              .map(ProteomicsMeasurementProperty::propertyName).collect(
                  Collectors.toSet()));
    }
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
    MS_DEVICE("ms device"),
    CYCLE("cycle/fraction name"),
    DIGESTION_METHOD("digestion method"),
    DIGESTION_ENZYME("digestion enzyme"),
    ENRICHMENT_METHOD("enrichment method"),
    INJECTION_VOLUME("injection volume (µl)"),
    LC_COLUMN("lc column"),
    LCMS_METHOD("lcms method"),
    LABELING_TYPE("labeling type"),
    LABEL("label"),
    COMMENT("comment");

    private final String name;

    ProteomicsMeasurementProperty(String value) {
      this.name = value;
    }

    static Optional<ProteomicsMeasurementProperty> fromString(String value) {
      var sanitizedValue = sanitizeValue(value);
      return Arrays.stream(ProteomicsMeasurementProperty.values())
          .filter(property -> property.propertyName().equals(sanitizedValue)).findFirst();
    }

    static boolean valueMatchesAnyProperty(String value) {
      var sanitizedValue = sanitizeValue(value);
      return Arrays.stream(ProteomicsMeasurementProperty.values())
          .map(ProteomicsMeasurementProperty::name)
          .anyMatch(sanitizedValue::equalsIgnoreCase);
    }

    public String propertyName() {
      return name;
    }

  }

  enum NGSMeasurementProperty {
    MEASUREMENT_ID("measurement id"),
    ORGANISATION_ID("organisation id"),
    SAMPLE_POOL_GROUP("sample pool group"),
    FACILITY("facility"),
    INSTRUMENT("instrument"),
    SEQUENCING_READ_TYPE("sequencing read type"),
    LIBRARY_KIT("library kit"),
    FLOW_CELL("flow cell"),
    SEQUENCING_RUN_PROTOCOL("sequencing run protocol"),
    INDEX_I7("index i7"),
    INDEX_I5("index i5"),
    COMMENT("comment");

    private final String name;

    NGSMeasurementProperty(String value) {
      this.name = value;
    }

    /**
     * Tries to convert an input property value to a known {@link NGSMeasurementProperty}.
     * <p>
     * Trailing whitespace will be ignored.
     *
     * @param value the presumed value to convert to a known {@link NGSMeasurementProperty}
     * @return the matching property, or {@link Optional#empty()}.
     * @since 1.4.0
     */
    static Optional<NGSMeasurementProperty> fromStringTrimmed(String value) {
      var sanitizedValue = sanitizeValue(value);
      return Arrays.stream(NGSMeasurementProperty.values())
          .filter(property -> property.propertyName().equalsIgnoreCase(sanitizedValue)).findFirst();
    }

    static boolean valueMatchesAnyProperty(String value) {
      var sanitizedValue = sanitizeValue(value);
      return Arrays.stream(NGSMeasurementProperty.values()).map(NGSMeasurementProperty::name)
          .anyMatch(sanitizedValue::equalsIgnoreCase);
    }

    String propertyName() {
      return name;
    }
  }
}
