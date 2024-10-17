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
import life.qbic.datamanager.parser.measurement.NGSMeasurementEditColumn;
import life.qbic.datamanager.parser.measurement.NGSMeasurementRegisterColumn;
import life.qbic.datamanager.parser.measurement.ProteomicsMeasurementEditColumn;
import life.qbic.datamanager.parser.measurement.ProteomicsMeasurementRegisterColumn;
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
 *   <li>Proteomics Measurement {@link ProteomicsMeasurementEditColumn}</li>
 *   <li>NGS Measurement {@link NGSMeasurementEditColumn}</li>
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
  public List<MeasurementMetadata> convertRegister(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, MissingSampleIdException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.columnMap().keySet();
    if (looksLikeNgsMeasurement(properties, false)) {
      return tryConversion(this::convertNewNGSMeasurement, parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, false)) {
      return tryConversion(this::convertNewProteomicsMeasurement, parsingResult);
    } else {
      throw new UnknownMetadataTypeException(
          "Unknown metadata type: cannot match properties to any known metadata type. Provided [%s]".formatted(
              String.join(", ", properties)));
    }
  }

  @Override
  public List<MeasurementMetadata> convertEdit(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, MissingSampleIdException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.columnMap().keySet();
    if (looksLikeNgsMeasurement(properties, true)) {
      return tryConversion(this::convertExistingNGSMeasurement, parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, true)) {
      return tryConversion(this::convertExistingProteomicsMeasurement, parsingResult);
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

  private List<MeasurementMetadata> convertNewProteomicsMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCode = SampleCode.create(parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.SAMPLE_ID.headerName(), ""));
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.TECHNICAL_REPLICATE_NAME.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.ORGANISATION_ID.headerName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.MS_DEVICE.headerName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.POOL_GROUP.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.FACILITY.headerName(), "");
      var fractionName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.CYCLE_FRACTION_NAME.headerName(), "");
      var digestionEnzyme = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.DIGESTION_ENZYME.headerName(), "");
      var digestionMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.headerName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.ENRICHMENT_METHOD.headerName(), "");
      var injectionVolume = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.INJECTION_VOLUME.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LC_COLUMN.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LCMS_METHOD.headerName(), "");
      var labelingType = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LABELING_TYPE.headerName(), "");
      var label = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LABEL.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.COMMENT.headerName(), "");
      var pxpMetaDaturm = new ProteomicsMeasurementMetadata(
          "",
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

  private List<MeasurementMetadata> convertExistingProteomicsMeasurement(
      ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleCode = SampleCode.create(parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.SAMPLE_ID.headerName(), ""));
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.TECHNICAL_REPLICATE_NAME.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.ORGANISATION_ID.headerName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.MS_DEVICE.headerName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.POOL_GROUP.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.FACILITY.headerName(), "");
      var fractionName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.CYCLE_FRACTION_NAME.headerName(), "");
      var digestionEnzyme = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.DIGESTION_ENZYME.headerName(), "");
      var digestionMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.DIGESTION_METHOD.headerName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.ENRICHMENT_METHOD.headerName(), "");
      var injectionVolume = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.INJECTION_VOLUME.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LC_COLUMN.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LCMS_METHOD.headerName(), "");
      var labelingType = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LABELING_TYPE.headerName(), "");
      var label = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LABEL.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.COMMENT.headerName(), "");
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

  private List<MeasurementMetadata> convertExistingNGSMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleCodes = List.of(
          SampleCode.create(
              parsingResult.getValueOrDefault(i, NGSMeasurementEditColumn.SAMPLE_ID.headerName(),
                  ""))
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.ORGANISATION_ID.headerName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INSTRUMENT.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.FACILITY.headerName(), "");
      var sequencingReadType = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.headerName(), "");
      var libraryKit = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.LIBRARY_KIT.headerName(), "");
      var flowCell = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.FLOW_CELL.headerName(), "");
      var runProtocol = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.SEQUENCING_RUN_PROTOCOL.headerName(), "");
      var poolGroup = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.POOL_GROUP.headerName(), "");
      var indexI7 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INDEX_I7.headerName(), "");
      var indexI5 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INDEX_I5.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.COMMENT.headerName(), "");
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

  private List<MeasurementMetadata> convertNewNGSMeasurement(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementMetadata>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCodes = List.of(
          SampleCode.create(
              parsingResult.getValueOrDefault(i,
                  NGSMeasurementRegisterColumn.SAMPLE_ID.headerName(),
                  ""))
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.ORGANISATION_ID.headerName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INSTRUMENT.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.FACILITY.headerName(), "");
      var sequencingReadType = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.headerName(), "");
      var libraryKit = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.LIBRARY_KIT.headerName(), "");
      var flowCell = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.FLOW_CELL.headerName(), "");
      var runProtocol = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.SEQUENCING_RUN_PROTOCOL.headerName(), "");
      var poolGroup = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.POOL_GROUP.headerName(), "");
      var indexI7 = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INDEX_I7.headerName(), "");
      var indexI5 = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INDEX_I5.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.COMMENT.headerName(), "");
      var metadatum = new NGSMeasurementMetadata(
          "",
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
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementRegisterColumn.values())
              .map(NGSMeasurementRegisterColumn::headerName)
              .map(Sanitizer::headerEncoder)
              .collect(Collectors.toSet()), NGSMeasurementEditColumn.MEASUREMENT_ID.headerName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(NGSMeasurementEditColumn.values())
              .map(NGSMeasurementEditColumn::headerName)
              .map(Sanitizer::headerEncoder)
              .collect(Collectors.toSet()));
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
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementRegisterColumn.values())
              .map(ProteomicsMeasurementRegisterColumn::headerName)
              .map(Sanitizer::headerEncoder)
              .collect(Collectors.toSet()),
          ProteomicsMeasurementEditColumn.MEASUREMENT_ID.headerName());
    } else {
      hitMap = countHits(formattedProperties,
          Arrays.stream(ProteomicsMeasurementEditColumn.values())
              .map(ProteomicsMeasurementEditColumn::headerName)
              .map(Sanitizer::headerEncoder)
              .collect(Collectors.toSet()));
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
