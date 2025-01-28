package life.qbic.datamanager.importing.parser;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementEditColumn;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementRegisterColumn;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementEditColumn;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementRegisterColumn;
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

  @Override
  public List<MeasurementMetadata> convertRegister(ParsingResult parsingResult)
      throws UnknownMetadataTypeException, MissingSampleIdException {
    Objects.requireNonNull(parsingResult);
    var properties = parsingResult.columnMap().keySet();
    if (looksLikeNgsMeasurement(properties, true)) {
      return tryConversion(this::convertNewNGSMeasurement, parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, true)) {
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
    if (looksLikeNgsMeasurement(properties, false)) {
      return tryConversion(this::convertExistingNGSMeasurement, parsingResult);
    } else if (looksLikeProteomicsMeasurement(properties, false)) {
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
          ProteomicsMeasurementRegisterColumn.SAMPLE_ID.getName(), ""));
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.TECHNICAL_REPLICATE_NAME.getName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.ORGANISATION_URL.getName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.MS_DEVICE.getName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.POOL_GROUP.getName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.FACILITY.getName(), "");
      var fractionName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.CYCLE_FRACTION_NAME.getName(), "");
      var digestionEnzyme = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.DIGESTION_ENZYME.getName(), "");
      var digestionMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.DIGESTION_METHOD.getName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.ENRICHMENT_METHOD.getName(), "");
      var injectionVolume = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.INJECTION_VOLUME.getName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LC_COLUMN.getName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LCMS_METHOD.getName(), "");
      var labelingType = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LABELING_TYPE.getName(), "");
      var label = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.LABEL.getName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.COMMENT.getName(), "");
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
          ProteomicsMeasurementEditColumn.MEASUREMENT_ID.getName(), "");
      var sampleCode = SampleCode.create(parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.SAMPLE_ID.getName(), ""));
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.TECHNICAL_REPLICATE_NAME.getName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.ORGANISATION_URL.getName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.MS_DEVICE.getName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.POOL_GROUP.getName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.FACILITY.getName(), "");
      var fractionName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.CYCLE_FRACTION_NAME.getName(), "");
      var digestionEnzyme = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.DIGESTION_ENZYME.getName(), "");
      var digestionMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.DIGESTION_METHOD.getName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.ENRICHMENT_METHOD.getName(), "");
      var injectionVolume = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.INJECTION_VOLUME.getName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LC_COLUMN.getName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LCMS_METHOD.getName(), "");
      var labelingType = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LABELING_TYPE.getName(), "");
      var label = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.LABEL.getName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.COMMENT.getName(), "");
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
          NGSMeasurementEditColumn.MEASUREMENT_ID.getName(), "");
      var sampleCodes = List.of(
          SampleCode.create(
              parsingResult.getValueOrDefault(i, NGSMeasurementEditColumn.SAMPLE_ID.getName(),
                  ""))
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.ORGANISATION_URL.getName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INSTRUMENT.getName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.FACILITY.getName(), "");
      var sequencingReadType = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.getName(), "");
      var libraryKit = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.LIBRARY_KIT.getName(), "");
      var flowCell = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.FLOW_CELL.getName(), "");
      var runProtocol = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.SEQUENCING_RUN_PROTOCOL.getName(), "");
      var poolGroup = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.POOL_GROUP.getName(), "");
      var indexI7 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INDEX_I7.getName(), "");
      var indexI5 = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.INDEX_I5.getName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.COMMENT.getName(), "");
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
                  NGSMeasurementRegisterColumn.SAMPLE_ID.getName(),
                  ""))
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.ORGANISATION_URL.getName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INSTRUMENT.getName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.FACILITY.getName(), "");
      var sequencingReadType = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.getName(), "");
      var libraryKit = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.LIBRARY_KIT.getName(), "");
      var flowCell = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.FLOW_CELL.getName(), "");
      var runProtocol = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.SEQUENCING_RUN_PROTOCOL.getName(), "");
      var poolGroup = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.POOL_GROUP.getName(), "");
      var indexI7 = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INDEX_I7.getName(), "");
      var indexI5 = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.INDEX_I5.getName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.COMMENT.getName(), "");
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
    var sanitizedColumnHeaders = properties.stream()
        .map(Sanitizer::headerEncoder)
        .collect(Collectors.toSet());
    Set<String> requiredColumnHeaders;
    if (ignoreID) {
      requiredColumnHeaders = Arrays.stream(NGSMeasurementRegisterColumn.values())
          .map(NGSMeasurementRegisterColumn::headerName)
          .map(Sanitizer::headerEncoder)
          .collect(Collectors.toSet());
    } else {
      requiredColumnHeaders = Arrays.stream(NGSMeasurementEditColumn.values())
          .map(NGSMeasurementEditColumn::headerName)
          .map(Sanitizer::headerEncoder)
          .collect(Collectors.toSet());
    }
    return hasAllRequiredProperties(requiredColumnHeaders, sanitizedColumnHeaders,
        "Missing properties for NGS measurement: %s");
  }

  private boolean looksLikeProteomicsMeasurement(Collection<String> properties, boolean ignoreID) {
    var sanitizedColumnHeaders = properties.stream()
        .map(Sanitizer::headerEncoder)
        .collect(Collectors.toSet());
    Set<String> requiredColumnHeaders;
    if (ignoreID) {
      requiredColumnHeaders = Arrays.stream(ProteomicsMeasurementRegisterColumn.values())
          .map(ProteomicsMeasurementRegisterColumn::getName)
          .map(Sanitizer::headerEncoder)
          .collect(Collectors.toSet());
    } else {
      requiredColumnHeaders = Arrays.stream(ProteomicsMeasurementEditColumn.values())
          .map(ProteomicsMeasurementEditColumn::getName)
          .map(Sanitizer::headerEncoder)
          .collect(Collectors.toSet());
    }
    return hasAllRequiredProperties(requiredColumnHeaders, sanitizedColumnHeaders,
        "Missing properties for proteomics measurement: %s");
  }

  private static boolean hasAllRequiredProperties(Set<String> requiredProperties,
      Set<String> presentProperties, String missingErrorMessage) {
    if (presentProperties.containsAll(requiredProperties)) {
      return true;
    }
    var missingProperties = new ArrayList<>();
    for (String requiredProperty : requiredProperties) {
      if (!presentProperties.contains(requiredProperty)) {
        missingProperties.add(requiredProperty);
      }
    }
    log.debug(missingErrorMessage.formatted(missingProperties));
    return false;
  }
}
