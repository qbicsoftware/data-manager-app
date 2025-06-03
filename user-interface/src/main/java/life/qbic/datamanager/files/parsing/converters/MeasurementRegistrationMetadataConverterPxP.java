package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.ProteomicsMeasurementRegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.measurement.Labeling;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementRegistrationMetadataConverterPxP implements
    MetadataConverterV2<MeasurementRegistrationInformationPxP> {

  @Override
  public List<MeasurementRegistrationInformationPxP> convert(ParsingResult parsingResult) {
    var result = new ArrayList<MeasurementRegistrationInformationPxP>();
    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCode = SampleCode.create(parsingResult.getValueOrDefault(i,
          ProteomicsMeasurementRegisterColumn.SAMPLE_ID.headerName(), ""));
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

      var metaDatum = new MeasurementRegistrationInformationPxP(
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
          comment
      );
      result.add(metaDatum);
    }
    return result;
  }
}
