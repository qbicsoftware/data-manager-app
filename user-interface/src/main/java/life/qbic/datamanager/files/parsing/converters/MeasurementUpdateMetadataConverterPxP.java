package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementEditColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.measurement.Labeling;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * Measurement Update Metadata Converter PxP
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementUpdateInformationPxP}.
 *
 * @since 1.10.0
 */
public class MeasurementUpdateMetadataConverterPxP implements
    MetadataConverterV2<MeasurementUpdateInformationPxP> {

  @Override
  public List<MeasurementUpdateInformationPxP> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<MeasurementUpdateInformationPxP>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.SAMPLE_ID.headerName(), "");
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.TECHNICAL_REPLICATE_NAME.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.ORGANISATION_URL.headerName(), "");
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
      var measurementName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.MEASUREMENT_NAME.headerName(), "");
      var comment = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementEditColumn.COMMENT.headerName(), "");

      var specificMetadata = new MeasurementSpecificPxP(label, fractionName, comment);

      var metaDatum = new MeasurementUpdateInformationPxP(
          measurementId,
          technicalReplicateName,
          organisationId,
          msDevice,
          samplePoolGroup,
          facility,
          digestionEnzyme,
          digestionMethod,
          enrichmentMethod,
          injectionVolume,
          lcColumn,
          lcmsMethod,
          labelingType,
          Map.of(sampleId, specificMetadata),
          measurementName
      );
      convertedElements.add(metaDatum);
    }
    return convertedElements;
  }
}
