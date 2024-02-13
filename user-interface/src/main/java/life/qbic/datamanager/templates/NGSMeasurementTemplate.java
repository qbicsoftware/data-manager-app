package life.qbic.datamanager.templates;

import java.io.IOException;
import java.util.Objects;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;

/**
 * <b>NGS measurement template</b>
 *
 * <p>The Excel spreadsheet containing the required information for measurement registration-</p>
 *
 * @since 1.0.0
 */
public class NGSMeasurementTemplate implements DownloadContentProvider {

  private static final String NGS_MEASUREMENT_TEMPLATE_PATH = "templates/ngs_measurement_registration_sheet.xlsx";

  private static final String NGS_MEASUREMENT_TEMPLATE_FILENAME = "ngs_measurement_registration_sheet.xlsx";

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
}
