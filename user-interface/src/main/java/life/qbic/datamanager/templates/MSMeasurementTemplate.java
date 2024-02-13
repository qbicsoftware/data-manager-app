package life.qbic.datamanager.templates;

import java.io.IOException;
import java.util.Objects;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;

/**
 * <b>MS measurement template</b>
 *
 * <p>The Excel spreadsheet containing the required information for mass spectrometry measurement
 * registration-</p>
 *
 * @since 1.0.0
 */
public class MSMeasurementTemplate implements DownloadContentProvider {

  private static final String MS_MEASUREMENT_TEMPLATE_PATH = "templates/ms_measurement_registration_sheet.xlsx";

  private static final String MS_MEASUREMENT_TEMPLATE_FILENAME = "ms_measurement_registration_sheet.xlsx";

  public MSMeasurementTemplate() {
  }

  @Override
  public byte[] getContent() {
    try {
      return Objects.requireNonNull(
              getClass().getClassLoader().getResourceAsStream(MS_MEASUREMENT_TEMPLATE_PATH))
          .readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException("Cannot get content for template: " + MS_MEASUREMENT_TEMPLATE_PATH,
          e);
    }
  }

  @Override
  public String getFileName() {
    return MS_MEASUREMENT_TEMPLATE_FILENAME;
  }
}
