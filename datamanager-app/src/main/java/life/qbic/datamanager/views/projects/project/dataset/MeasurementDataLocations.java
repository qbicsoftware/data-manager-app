package life.qbic.datamanager.views.projects.project.dataset;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;

/**
 * <b>The list of data locations for a set of measurements, as a downloadable file.</b>
 *
 * <p>The application does not serve measurement data: the files are produced by the facility, held
 * by a separate download service, and this application only knows that they exist. "Downloading a
 * dataset" therefore means exporting <em>where</em> its files are -- one URL per measurement -- and
 * fetching them with a personal access token.</p>
 *
 * <p><b>Why this is shared.</b> Two surfaces now offer that export: the experiment's Datasets page,
 * scoped to the dataset the reader has selected, and the project's Datasets page, scoped to one row
 * of the measurement-dataset table. They must produce the <em>same</em> file -- same URL shape, same
 * ordering, same trailing newline, same file name -- or the same dataset exports differently
 * depending on where the reader happened to click. The construction lives here once so it cannot
 * drift.</p>
 *
 * <p><b>Known debt, unchanged.</b> The URL is built from a configured endpoint prefix
 * ({@code server.download.api.measurement.url}), which asserts a contract with a service this
 * application neither owns nor can verify. A later increment replaces construction with discovery.
 * That debt is stated once, here, rather than at each call site.</p>
 *
 * @since 1.22.0
 */
public final class MeasurementDataLocations {

  /**
   * The prefix of a measurement's data URL. Empty when not configured, which yields URLs that are
   * paths only -- the failure is then visible in the exported file rather than silent.
   */
  private final String endpoint;

  public MeasurementDataLocations(String endpoint) {
    this.endpoint = endpoint == null ? "" : endpoint;
  }

  /**
   * The data URL for one measurement.
   *
   * @param measurementCode the code the download service is keyed by
   */
  public String urlFor(String measurementCode) {
    return endpoint + "/" + measurementCode;
  }

  /**
   * The location list as a text file, one URL per line.
   *
   * @param measurementCodes the measurements to export, in the order they should appear
   * @param projectCode      the project code for the file name
   * @param datasetLabel     what the export covers, for the file name (e.g. "WGS" or
   *                         "measurement_dataset"); sanitised by the formatter
   * @return the provider to hand to a download component, or {@code null} when there is nothing to
   *     export -- callers turn that into their own message, because each knows the right wording
   */
  public DownloadStreamProvider locationList(List<String> measurementCodes, String projectCode,
      String datasetLabel) {
    if (measurementCodes == null || measurementCodes.isEmpty()) {
      return null;
    }
    byte[] content = measurementCodes.stream()
        .map(this::urlFor)
        .collect(java.util.stream.Collectors.joining("\n", "", "\n"))
        .getBytes(StandardCharsets.UTF_8);
    String name = FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(),
        projectCode == null || projectCode.isBlank() ? "project" : projectCode,
        datasetLabel == null || datasetLabel.isBlank() ? "measurement_dataset" : datasetLabel,
        "txt");
    return new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return name;
      }

      @Override
      public InputStream getStream() {
        return new ByteArrayInputStream(content);
      }

      @Override
      public String getContentType() {
        return org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.of((long) content.length);
      }
    };
  }
}
