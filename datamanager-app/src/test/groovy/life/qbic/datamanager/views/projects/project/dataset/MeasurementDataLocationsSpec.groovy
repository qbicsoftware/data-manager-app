package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification

import java.nio.charset.StandardCharsets

/**
 * Covers the data-location export both dataset surfaces produce.
 *
 * <p>"Downloading a dataset" in this application means exporting <em>where</em> its files are: the
 * application does not serve the data, so it emits one URL per measurement for the reader to fetch
 * with a personal access token. Two pages now offer that export -- the experiment's Datasets page
 * (scoped to the selected dataset) and the project's Datasets table (scoped to one row) -- so the
 * construction is shared. If it were duplicated the same dataset would export differently depending
 * on which page the reader happened to use.</p>
 */
class MeasurementDataLocationsSpec extends Specification {

  def "the list is one URL per measurement, one per line, with a trailing newline"() {
    given:
    def locations = new MeasurementDataLocations("https://data.example.org/measurements")

    when:
    def content = text(locations.locationList(["NGS-1", "NGS-2"], "Q2C00L", "WGS"))

    then: "each line is the endpoint joined to the measurement code"
    content.readLines() == [
        "https://data.example.org/measurements/NGS-1",
        "https://data.example.org/measurements/NGS-2",
    ]

    and: "the file ends with a newline, so the last line is not truncated by tools that expect one"
    content.endsWith("\n")
  }

  /**
   * The ordering is the caller's, not the set's: the same dataset exported from either page must
   * produce a byte-identical file, so nothing here re-sorts.
   */
  def "the order the caller supplies is preserved"() {
    given:
    def locations = new MeasurementDataLocations("https://e")

    when:
    def content = text(locations.locationList(["C", "A", "B"], "Q2C00L", "WGS"))

    then:
    content.readLines() == ["https://e/C", "https://e/A", "https://e/B"]
  }

  def "there is nothing to export when no measurement carries data"() {
    given:
    def locations = new MeasurementDataLocations("https://e")

    expect: "the caller turns this into its own message rather than downloading an empty file"
    locations.locationList([], "Q2C00L", "WGS") == null
    locations.locationList(null, "Q2C00L", "WGS") == null
  }

  def "the file is named from the project code and the dataset, not from an internal id"() {
    given:
    def locations = new MeasurementDataLocations("https://e")

    when:
    def filename = locations.locationList(["M-1"], "Q2C00L", "WGS").getFilename()

    then: "the readable project code and the dataset label both appear"
    filename.contains("Q2C00L")
    filename.contains("WGS")
    filename.endsWith(".txt")
  }

  def "a missing project code or label degrades to a fallback rather than to a broken name"() {
    given:
    def locations = new MeasurementDataLocations("https://e")

    when:
    def filename = locations.locationList(["M-1"], null, null).getFilename()

    then:
    filename.contains("project")
    filename.contains("measurement_dataset")
    filename.endsWith(".txt")
  }

  /**
   * The endpoint is a configured value. When it is absent the export still runs and writes paths
   * only, so the misconfiguration is visible in the file rather than swallowed.
   */
  def "an unconfigured endpoint yields paths rather than failing the export"() {
    given:
    def locations = new MeasurementDataLocations("")

    expect:
    locations.urlFor("NGS-1") == "/NGS-1"
    text(locations.locationList(["NGS-1"], "Q2C00L", "WGS")) == "/NGS-1\n"
  }

  private static String text(
      life.qbic.datamanager.files.export.download.DownloadStreamProvider provider) {
    return provider.getStream().getText(StandardCharsets.UTF_8.name())
  }
}
