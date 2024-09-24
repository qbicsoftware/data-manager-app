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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.parser.measurement.NGSMeasurementEditColumns;
import life.qbic.datamanager.parser.measurement.ProteomicsMeasurementEditColumns;
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
 *   <li>Proteomics Measurement {@link ProteomicsMeasurementEditColumns}</li>
 *   <li>NGS Measurement {@link NGSMeasurementEditColumns}</li>
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

  @Override
  public List<MeasurementMetadata> convert(ParsingResult parsingResult, boolean ignoreMeasurementId)
      throws UnknownMetadataTypeException, MissingSampleIdException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.columnMap().keySet();
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
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.MEASUREMENT_ID.headerName(), "");
      var sampleCode = SampleCode.create(parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.SAMPLE_ID.headerName(), ""));
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.TECHNICAL_REPLICATE_NAME.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.ORGANISATION_ID.headerName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.MS_DEVICE.headerName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.POOL_GROUP.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.FACILITY.headerName(), "");
      var fractionName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.CYCLE_FRACTION_NAME.headerName(), "");
      var digestionEnzyme = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.DIGESTION_ENZYME.headerName(), "");
      var digestionMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.DIGESTION_METHOD.headerName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.ENRICHMENT_METHOD.headerName(), "");
      var injectionVolume = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.INJECTION_VOLUME.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.LC_COLUMN.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.LCMS_METHOD.headerName(), "");
      var labelingType = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.LABELING_TYPE.headerName(), "");
      var label = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.LABEL.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumns.COMMENT.headerName(), "");
      var pxpMetaDaturm = new ProteomicsMeasurementMetadata(measurementId,
          sampleCode,
          technicalReplicateName,
          organisationId,
          msDevice,
          samplePoolGroup,
          facility,
          fractionName,
          digestionEnzyme,
          digestionMethod,
          enrichmentMethod,
          injectionVolume,
          lcColumn,
          lcmsMethod,
          new Labeling(labelingType, label),
          comment);
      result.add(pxpMetaDaturm);
    }
    return result;
  }

  private List<MeasurementMetadata> convertNGSMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.MEASUREMENT_ID.headerName(), "");
      var sampleCodes = List.of(
          SampleCode.create(
              parsingResult.getValueOrDefault(i, NGSMeasurementEditColumns.SAMPLE_ID.headerName(),
                  ""))
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.ORGANISATION_ID.headerName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.INSTRUMENT.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.FACILITY.headerName(), "");
      var sequencingReadType = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.SEQUENCING_RUN_PROTOCOL.headerName(), "");
      var libraryKit = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.LIBRARY_KIT.headerName(), "");
      var flowCell = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.FLOW_CELL.headerName(), "");
      var runProtocol = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.SEQUENCING_RUN_PROTOCOL.headerName(), "");
      var poolGroup = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.POOL_GROUP.headerName(), "");
      var indexI7 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.INDEX_I7.headerName(), "");
      var indexI5 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.INDEX_I5.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumns.COMMENT.headerName(), "");
      var metadatum = new NGSMeasurementMetadata(
          measurementId,
          sampleCodes,
          organisationId,
          instrument,
          facility,
          sequencingReadType,
          libraryKit,
          flowCell,
          runProtocol,
          poolGroup,
          indexI7,
          indexI5,
          comment
      );
      result.add(metadatum);
    }
    return result;
  }

  private boolean looksLikeNgsMeasurement(Collection<String> properties, boolean ignoreID) {
    var formattedProperties = properties.stream().map(String::toLowerCase)
        .collect(Collectors.toList());
    Map<String, Integer> hitMap;
    if (ignoreID) {
      formattedProperties.remove(NGSMeasurementEditColumns.MEASUREMENT_ID.headerName());
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementEditColumns.values())
              .map(NGSMeasurementEditColumns::headerName)
              .collect(
                  Collectors.toSet()), NGSMeasurementEditColumns.MEASUREMENT_ID.headerName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementEditColumns.values())
              .map(NGSMeasurementEditColumns::headerName).collect(
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
      formattedProperties.remove(ProteomicsMeasurementEditColumns.MEASUREMENT_ID.headerName());
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementEditColumns.values())
              .map(ProteomicsMeasurementEditColumns::headerName).collect(
                  Collectors.toSet()), ProteomicsMeasurementEditColumns.MEASUREMENT_ID.headerName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementEditColumns.values())
              .map(ProteomicsMeasurementEditColumns::headerName).collect(
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
}
