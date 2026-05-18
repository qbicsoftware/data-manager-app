package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.IPMeasurementEditColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificIP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationIP;

/**
 * Measurement Update Metadata Converter IP
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementUpdateInformationIP}.
 *
 * @since 1.11.0
 */
public class MeasurementUpdateMetadataConverterIP implements
    MetadataConverterV2<MeasurementUpdateInformationIP> {

  @Override
  public List<MeasurementUpdateInformationIP> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<MeasurementUpdateInformationIP>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleId = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.SAMPLE_ID.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.ORGANISATION_URL.headerName(), "");
      var instrument = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.INSTRUMENT.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.FACILITY.headerName(), "");
      var samplePoolGroup = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.SAMPLE_NAME.headerName(), "");
      var measurementName = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MEASUREMENT_NAME.headerName(), "");

      // Sample-specific metadata
      var sampleMass = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.SAMPLE_MASS.headerName(), "");
      var sampleVolume = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.SAMPLE_VOLUME.headerName(), "");
      var cycleFractionName = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.CYCLE_FRACTION_NAME.headerName(), "");
      var mhcAntibody = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MHC_ANTIBODY.headerName(), "");
      var mhcTypingMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MHC_TYPING_METHOD.headerName(), "");
      var enrichmentMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.ENRICHMENT_METHOD.headerName(), "");
      var prepDate = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.PREP_DATE.headerName(), "");
      var msRunDate = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MS_RUN_DATE.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.LCMS_METHOD.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.LC_COLUMN.headerName(), "");
      var dataAcquisition = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.DATA_ACQUISITION.headerName(), "");
      var massRange = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.MASS_RANGE.headerName(), "");
      var retentionTimeRange = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.RETENTION_TIME_RANGE.headerName(), "");
      var chargeRange = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.CHARGE_RANGE.headerName(), "");
      var ionMobilityRange = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.ION_MOBILITY_RANGE.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          IPMeasurementEditColumn.COMMENT.headerName(), "");

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

      var metaDatum = new MeasurementUpdateInformationIP(
          measurementId,
          organisationId,
          instrument,
          facility,
          samplePoolGroup,
          Map.of(sampleId, specificMetadata),
          measurementName
      );
      convertedElements.add(metaDatum);
    }
    return convertedElements;
  }
}