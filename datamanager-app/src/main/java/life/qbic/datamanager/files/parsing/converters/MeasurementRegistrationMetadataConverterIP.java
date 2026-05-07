package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.IPMeasurementRegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificIP;

/**
 * Measurement Registration Metadata Converter Immunopeptidomics
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementRegistrationInformationIP}.
 *
 * @since 1.11.0
 */
public class MeasurementRegistrationMetadataConverterIP implements
    MetadataConverterV2<MeasurementRegistrationInformationIP> {

  @Override
  public List<MeasurementRegistrationInformationIP> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<MeasurementRegistrationInformationIP>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleId = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.SAMPLE_ID.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.ORGANISATION_URL.headerName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.INSTRUMENT.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.FACILITY.headerName(), "");
      var sampleMass = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.SAMPLE_MASS.headerName(), "");
      var sampleVolume = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.SAMPLE_VOLUME.headerName(), "");
      var cycleFractionName = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.CYCLE_FRACTION_NAME.headerName(), "");
      var mhcAntibody = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.MHC_ANTIBODY.headerName(), "");
      var mhcTypingMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.MHC_TYPING_METHOD.headerName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.ENRICHMENT_METHOD.headerName(), "");
      var prepDate = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.PREP_DATE.headerName(), "");
      var msRunDate = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.MS_RUN_DATE.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.LCMS_METHOD.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.LC_COLUMN.headerName(), "");
      var dataAcquisition = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.DATA_ACQUISITION.headerName(), "");
      var massRange = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.MASS_RANGE.headerName(), "");
      var retentionTimeRange = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.RETENTION_TIME_RANGE.headerName(), "");
      var chargeRange = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.CHARGE_RANGE.headerName(), "");
      var ionMobilityRange = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.ION_MOBILITY_RANGE.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.COMMENT.headerName(), "");
      var measurementName = parsingResult.getValueOrDefault(i,
          IPMeasurementRegisterColumn.MEASUREMENT_NAME.headerName(), "");

      var specificMetadata = new MeasurementSpecificIP(
          sampleMass,
          sampleVolume,
          cycleFractionName,
          mhcAntibody,
          mhcTypingMethod,
          enrichmentMethod,
          prepDate,
          msRunDate,
          lcmsMethod,
          lcColumn,
          dataAcquisition,
          massRange,
          retentionTimeRange,
          chargeRange,
          ionMobilityRange,
          comment
      );

      var metadatum = new MeasurementRegistrationInformationIP(
          organisationId,
          instrument,
          facility,
          "", // pool group - empty for now, can be added if needed
          Map.of(sampleId, specificMetadata),
          measurementName
      );
      convertedElements.add(metadatum);
    }
    return convertedElements;
  }
}
