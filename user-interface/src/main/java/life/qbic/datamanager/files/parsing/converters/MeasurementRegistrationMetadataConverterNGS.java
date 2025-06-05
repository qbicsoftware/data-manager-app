package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementRegisterColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;

/**
 * Measurement Registration Metadata Converter NGS
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementRegistrationInformationNGS}}.
 *
 * @since 1.10.0
 */
public class MeasurementRegistrationMetadataConverterNGS implements
    MetadataConverterV2<MeasurementRegistrationInformationNGS> {

  @Override
  public List<MeasurementRegistrationInformationNGS> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<MeasurementRegistrationInformationNGS>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var sampleCodes = List.of(
          parsingResult.getValueOrDefault(i,
              NGSMeasurementRegisterColumn.SAMPLE_ID.headerName(),
              "")
      );
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementRegisterColumn.ORGANISATION_URL.headerName(), "");
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

      var metadatum = new MeasurementRegistrationInformationNGS(
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
      convertedElements.add(metadatum);
    }
    return convertedElements;
  }
}
