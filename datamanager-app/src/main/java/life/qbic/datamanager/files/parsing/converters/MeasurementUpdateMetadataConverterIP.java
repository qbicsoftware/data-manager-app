package life.qbic.datamanager.files.parsing.converters;

import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationIP;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ImmunoPeptidomicsEditColumn;

/**
 * Measurement Update Metadata Converter IP
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementUpdateInformationIP}.
 * <p>
 * Date fields ({@link ImmunoPeptidomicsEditColumn#PREP_DATE} and
 * {@link ImmunoPeptidomicsEditColumn#MS_RUN_DATE}) are expected in ISO-8601 format (e.g.
 * {@code 2025-01-05}). Blank or unparseable values are converted to {@code null}.
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
          ImmunoPeptidomicsEditColumn.MEASUREMENT_ID.headerName(), "");
      var sampleId = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.SAMPLE_ID.headerName(), "");
      var sampleName = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.SAMPLE_NAME.headerName(), "");
      var measurementName = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.MEASUREMENT_NAME.headerName(), "");
      var organisationUrl = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.ORGANISATION_URL.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.FACILITY.headerName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.MS_DEVICE.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.LCMS_METHOD.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.LC_COLUMN.headerName(), "");
      var mhcAntibody = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.MHC_ANTIBODY.headerName(), "");
      var samplePrepDate = DateTimeConverter.parseInstant(DateTimeFormat.ISO_LOCAL_DATE,
          parsingResult.getValueOrDefault(i,
              ImmunoPeptidomicsEditColumn.PREP_DATE.headerName(), ""));
      var msRunDate = DateTimeConverter.parseInstant(DateTimeFormat.ISO_LOCAL_DATE,
          parsingResult.getValueOrDefault(i,
              ImmunoPeptidomicsEditColumn.MS_RUN_DATE.headerName(), ""));
      var comment = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsEditColumn.COMMENT.headerName(), "");

      convertedElements.add(new MeasurementUpdateInformationIP(
          measurementId,
          sampleId,
          sampleName,
          measurementName,
          organisationUrl,
          facility,
          msDevice,
          lcmsMethod,
          lcColumn,
          mhcAntibody,
          samplePrepDate,
          msRunDate,
          comment
      ));
    }
    return convertedElements;
  }
}
