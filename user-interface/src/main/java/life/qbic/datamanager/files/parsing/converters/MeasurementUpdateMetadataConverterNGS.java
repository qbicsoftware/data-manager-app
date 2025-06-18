package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.structure.measurement.NGSMeasurementEditColumn;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;

/**
 * Measurement Update Metadata Converter NGS
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementUpdateInformationNGS}}.
 *
 * @since 1.10.0
 */
public class MeasurementUpdateMetadataConverterNGS implements
    MetadataConverterV2<MeasurementUpdateInformationNGS> {

  @Override
  public List<MeasurementUpdateInformationNGS> convert(ParsingResult parsingResult) {
    var convertedElements = new ArrayList<MeasurementUpdateInformationNGS>();

    for (int i = 0; i < parsingResult.rows().size(); i++) {
      var measurementId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.SAMPLE_ID.headerName(),
          "");
      var organisationId = parsingResult.getValueOrDefault(i,
          NGSMeasurementEditColumn.ORGANISATION_URL.headerName(), "");
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

      var specificMetadata = new MeasurementSpecificNGS(indexI7, indexI5, comment);

      var metaDatum = new MeasurementUpdateInformationNGS(
          measurementId,
          organisationId,
          instrument,
          facility,
          sequencingReadType,
          libraryKit,
          flowCell,
          runProtocol,
          poolGroup,
          Map.of(sampleId, specificMetadata)
      );
      convertedElements.add(metaDatum);
    }
    return convertedElements;
  }
}
