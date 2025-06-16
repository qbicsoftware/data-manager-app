package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementRegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP;

/**
 * Measurement Registration Metadata Converter PxP
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementRegistrationInformationPxP}.
 *
 * @since 1.10.0
 */
public class MeasurementRegistrationMetadataConverterPxP implements
    MetadataConverterV2<MeasurementRegistrationInformationPxP> {

  @Override
  public List<MeasurementRegistrationInformationPxP> convert(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementRegistrationInformationPxP>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.SAMPLE_ID.headerName(), "");
      var technicalReplicateName = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.TECHNICAL_REPLICATE_NAME.headerName(), "");
      var organisationId = parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.ORGANISATION_URL.headerName(), "");
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

      var specificMetadata = new MeasurementSpecificPxP(label, fractionName, comment);

      var metaDatum = new MeasurementRegistrationInformationPxP(
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
          Map.of(sampleId, specificMetadata)
      );
      result.add(metaDatum);
    }
    return result;
  }
}
