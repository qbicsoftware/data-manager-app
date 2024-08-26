package life.qbic.datamanager.parser;

import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.COMMENT;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.CYCLE;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.DIGESTION_ENZYME;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.DIGESTION_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.ENRICHMENT_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.FACILITY;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.INJECTION_VOLUME;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.INSTRUMENT;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LABEL;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LABELING_TYPE;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LCMS_METHOD;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.LC_COLUMN;
import static life.qbic.datamanager.parser.MetadataConverter.ProteomicsMeasurementProperty.MEASUREMENT_ID;
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
import java.util.stream.Collectors;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementProperty;
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
  private static Map<String, Integer> countHits(Collection<String> target, Set<String> hitValues) {
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
    return hits;
  }

  @Override
  public List<MeasurementMetadata> convert(ParsingResult parsingResult, boolean ignoreMeasurementId)
      throws UnknownMetadataTypeException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.keys().keySet();
    if (looksLikeNgsMeasurement(properties, ignoreMeasurementId)) {
      return convertNGSMeasurement(parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, ignoreMeasurementId)) {
      return convertProteomicsMeasurement(parsingResult);
    } else {
      throw new UnknownMetadataTypeException(
          "Unknown metadata type: cannot match properties to any known metadata type. Provided [%s]".formatted(
              String.join(", ", properties)));
    }
  }

  private List<MeasurementMetadata> convertProteomicsMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();
    var keyIndices = parsingResult.keys();
    var valuesIterator = parsingResult.values().iterator();
    while (valuesIterator.hasNext()) {
      var valueEntry = valuesIterator.next();
      var pxpMetaDatum = new ProteomicsMeasurementMetadata(
          valueEntry.get(keyIndices.get(MEASUREMENT_ID.propertyName())),
          SampleCode.create(valueEntry.get(keyIndices.get(QBIC_SAMPLE_ID.propertyName()))),
          valueEntry.get(keyIndices.get(ORGANISATION_ID.propertyName())),
          valueEntry.get(keyIndices.get(INSTRUMENT.propertyName())),
          valueEntry.get(keyIndices.get(SAMPLE_POOL_GROUP.propertyName())),
          valueEntry.get(keyIndices.get(FACILITY.propertyName())),
          valueEntry.get(keyIndices.get(CYCLE.propertyName())),
          valueEntry.get(keyIndices.get(DIGESTION_ENZYME.propertyName())),
          valueEntry.get(keyIndices.get(DIGESTION_METHOD.propertyName())),
          valueEntry.get(keyIndices.get(ENRICHMENT_METHOD.propertyName())),
          valueEntry.get(keyIndices.get(INJECTION_VOLUME.propertyName())),
          valueEntry.get(keyIndices.get(LC_COLUMN.propertyName())),
          valueEntry.get(keyIndices.get(LCMS_METHOD.propertyName())),
          new Labeling(valueEntry.get(keyIndices.get(LABELING_TYPE.propertyName())),
              valueEntry.get(keyIndices.get(LABEL.propertyName()))),
          valueEntry.get(keyIndices.get(COMMENT.propertyName()))
      );
      result.add(pxpMetaDatum);
    }
   return result;
  }

  private List<MeasurementMetadata> convertNGSMeasurement(ParsingResult parsingResult) {
    throw new RuntimeException("not implemented yet");
  }

  private boolean looksLikeNgsMeasurement(Collection<String> properties, boolean ignoreID) {
    var formattedProperties = properties.stream().map(String::toLowerCase).collect(Collectors.toList());
    if (ignoreID) {
      formattedProperties.remove(MEASUREMENT_ID.propertyName());
    }
    var hitMap = countHits(formattedProperties,
        Arrays.stream(NGSMeasurementProperty.values())
            .map(NGSMeasurementProperty::propertyName).collect(
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
      log.debug("Missing properties for NGS measurement: %s".formatted(missingProperties));
    }
    return false;
  }

  private boolean looksLikeProteomicsMeasurement(Collection<String> properties, boolean ignoreID) {
    var formattedProperties = properties.stream().map(String::toLowerCase).collect(Collectors.toList());
    if (ignoreID) {
      formattedProperties.remove(MEASUREMENT_ID.propertyName());
    }var hitMap = countHits(formattedProperties,
        Arrays.stream(ProteomicsMeasurementProperty.values())
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
    static Optional<NGSMeasurementProperty> fromStringTrailingIgnored(String value) {
      var trimmedValue = value.trim();
      return Arrays.stream(NGSMeasurementProperty.values())
          .filter(property -> property.propertyName().equalsIgnoreCase(trimmedValue)).findFirst();
    }

    static boolean valueMatchesAnyProperty(String value) {
      var trimmedValue = value.trim();
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
