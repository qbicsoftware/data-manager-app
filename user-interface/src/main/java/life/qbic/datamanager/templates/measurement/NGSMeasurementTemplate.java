package life.qbic.datamanager.templates.measurement;

import java.io.IOException;
import java.util.Objects;
import life.qbic.datamanager.templates.Template;

/**
 * <b>NGS measurement template</b>
 *
 * <p>The Excel spreadsheet containing the required information for measurement registration-</p>
 *
 * @since 1.0.0
 */
public class NGSMeasurementTemplate extends Template {

  private static final String NGS_MEASUREMENT_TEMPLATE_PATH = "templates/ngs_measurement_registration_sheet.xlsx";

  private static final String NGS_MEASUREMENT_TEMPLATE_FILENAME = "ngs_measurement_registration_sheet.xlsx";

  private static final String NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME = "Genomics Template";

  @Override
  public byte[] getContent() {
    try {
      return Objects.requireNonNull(
              getClass().getClassLoader().getResourceAsStream(NGS_MEASUREMENT_TEMPLATE_PATH))
          .readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot get content for template: " + NGS_MEASUREMENT_TEMPLATE_PATH,
          e);
    }
  }

  @Override
  public String getFileName() {
    return NGS_MEASUREMENT_TEMPLATE_FILENAME;
  }

  @Override
  public String getDomainName() {
    return NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME;
  }
}
