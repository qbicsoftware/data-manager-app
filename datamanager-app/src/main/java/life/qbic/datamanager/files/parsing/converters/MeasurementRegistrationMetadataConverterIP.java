package life.qbic.datamanager.files.parsing.converters;

import static life.qbic.datamanager.files.parsing.converters.DateTimeConverter.parseInstant;

import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationIP;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ImmunoPeptidomicsRegisterColumn;

/**
 * Measurement Registration Metadata Converter IP
 * <p>
 * Converter that converts a {@link ParsingResult} into a list of
 * {@link MeasurementRegistrationInformationIP}.
 * <p>
 * Date fields ({@link ImmunoPeptidomicsRegisterColumn#PREP_DATE} and
 * {@link ImmunoPeptidomicsRegisterColumn#MS_RUN_DATE}) are expected in ISO-8601 format (e.g.
 * {@code 2025-01-05}). Blank or unparseable values are converted to {@code null}.
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
          ImmunoPeptidomicsRegisterColumn.SAMPLE_ID.headerName(), "");
      var sampleName = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.SAMPLE_NAME.headerName(), "");
      var measurementName = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.MEASUREMENT_NAME.headerName(), "");
      var organisationUrl = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.ORGANISATION_URL.headerName(), "");
      var facility = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.FACILITY.headerName(), "");
      var msDevice = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.MS_DEVICE.headerName(), "");
      var lcmsMethod = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.LCMS_METHOD.headerName(), "");
      var lcColumn = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.LC_COLUMN.headerName(), "");
      var mhcAntibody = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.MHC_ANTIBODY.headerName(), "");
      var samplePrepDate = parseInstant(DateTimeFormat.ISO_LOCAL_DATE,
          parsingResult.getValueOrDefault(i,
              ImmunoPeptidomicsRegisterColumn.PREP_DATE.headerName(), ""));
      var msRunDate = parseInstant(DateTimeFormat.ISO_LOCAL_DATE, parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.MS_RUN_DATE.headerName(), ""));
      var comment = parsingResult.getValueOrDefault(i,
          ImmunoPeptidomicsRegisterColumn.COMMENT.headerName(), "");

      convertedElements.add(new MeasurementRegistrationInformationIP(
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
