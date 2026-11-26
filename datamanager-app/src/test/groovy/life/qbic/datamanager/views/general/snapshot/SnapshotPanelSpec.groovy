package life.qbic.datamanager.views.general.snapshot

import life.qbic.datamanager.views.general.DataStateLabel
import life.qbic.projectmanagement.application.query.AssayStanding
import life.qbic.projectmanagement.application.query.AssayType
import life.qbic.projectmanagement.application.query.DataState
import life.qbic.projectmanagement.application.query.ExperimentProgress
import life.qbic.projectmanagement.application.query.RecentMeasurement
import spock.lang.Specification

import java.time.Instant

/**
 * Covers the shared snapshot panel and the two dataset panels built from it.
 *
 * <p>The experiment overview and the project overview describe the same two data sets at the same
 * grain, and they had drifted: the project page retired its completeness ring and its
 * "Not complete yet" vocabulary, the experiment overview kept both, and the two tables laid out
 * differently. This spec pins the decisions that stopped applying to only one of the pages —
 * dataset grain, one numeric statement, the shared {@link DataState} words — so a future edit to
 * either page fails here instead of silently re-introducing a dialect.</p>
 */
class SnapshotPanelSpec extends Specification {

  // ── The dataset panel's grain ──────────────────────────────────────────

  /**
   * The card used to preview the three newest <em>measurements</em>. "Not complete" is a condition
   * of a dataset, so a list of measurement IDs could not say which dataset was behind.
   */
  def "the dataset panel states one row per dataset, not per measurement"() {
    given: "one experiment with two datasets"
    def progress = progress(8, 12, 6)
    def assays = [
        assay(AssayType.GENOMICS, "WGS", "Illumina", 6, 1),
        assay(AssayType.PROTEOMICS, "WGS", "Illumina", 6, 1),
    ]

    when:
    def panel = SnapshotPanel.datasets(progress, [], assays, false, "/datasets", "")

    then: "one row per dataset, and the columns are the dataset's identity"
    def rows = dataRows(panel)
    rows.size() == 2
    rows[0] == ["Genomics", "WGS", "Illumina", "1 of 6", "Partial"]
    rows[1] == ["Proteomics", "WGS", "Illumina", "1 of 6", "Partial"]

    and: "the metric counts datasets, which is what a row is"
    metricValue(panel) == "2"
    metricUnit(panel) == "datasets"
  }

  def "a dataset that records no instrument shows an em dash rather than substituting the method"() {
    given:
    def assays = [assay(AssayType.GENOMICS, "WGS", null, 3, 3)]

    when:
    def rows = dataRows(SnapshotPanel.datasets(progress(1, 3, 3), [], assays, false, "/d", ""))

    then: "the cell says 'not recorded' instead of claiming the method is an instrument"
    rows[0][2] == "\u2014"
  }

  // ── One numeric statement ──────────────────────────────────────────────

  /**
   * The experiment overview carried the count, a completeness ring and a gap sentence: three
   * encodings of one fact. The project overview settled on stating the ratio once, in words, and
   * the ring is not used in these panels at all.
   */
  def "the panel states the outstanding datasets in words and carries no completeness ring"() {
    given:
    def assays = [
        assay(AssayType.GENOMICS, "WGS", "Illumina", 6, 1),
        assay(AssayType.PROTEOMICS, "WGS", "Illumina", 6, 6),
    ]

    when:
    def panel = SnapshotPanel.datasets(progress(9, 12, 7), [], assays, false, "/d", "")

    then: "the gap is stated once, as a sentence under the count"
    qualifier(panel) == "1 of 2 datasets need primary data"

    and: "no ring is rendered"
    !containsClass(panel, "completeness-ring")
  }

  def "a completely delivered panel states no gap rather than a zero"() {
    given:
    def assays = [assay(AssayType.GENOMICS, "WGS", "Illumina", 4, 4)]

    when:
    def panel = SnapshotPanel.datasets(progress(4, 4, 4), [], assays, false, "/d", "")

    then:
    qualifier(panel) == ""
  }

  // ── The shared state vocabulary ────────────────────────────────────────

  /**
   * The experiment overview said "Registered", "Not complete yet" and "Not attached" while reading
   * the same {@link DataState} the project page renders as Available / Partial / Waiting.
   */
  def "dataset rows use the shared data-state vocabulary, not a page-local dialect"() {
    given:
    def assays = [
        assay(AssayType.GENOMICS, "WGS", "Illumina", 6, 6),
        assay(AssayType.PROTEOMICS, "WGS", "Illumina", 6, 3),
        assay(AssayType.IMMUNOPEPTIDOMICS, "WGS", "Illumina", 6, 0),
    ]

    when:
    def rows = dataRows(SnapshotPanel.datasets(progress(9, 18, 9), [], assays, false, "/d", ""))

    then:
    rows*.get(4) == ["Available", "Partial", "Waiting"]
  }

  def "the samples panel tags a started experiment with the same word the project page uses"() {
    given:
    def progress = progress(8, 0, 0)

    when:
    def panel = SnapshotPanel.samples(progress, [], "/samples", "")

    then:
    stateTag(panel) == DataStateLabel.word(DataState.COMPLETE)
    stateTag(panel) == "Available"
  }

  // ── Structure both pages rely on ───────────────────────────────────────

  /** The title is the panel's one destination; a separate "Open …" link duplicated it. */
  def "the title is the panel's link and there is no second open link"() {
    when:
    def panel = SnapshotPanel.samples(progress(3, 3, 0), [], "/projects/p/samples", "")

    then:
    def head = panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-head") }
        .findFirst().orElseThrow()
    def label = head.getChildren()
        .filter { it.classNames.contains("snapshot-panel-label") }
        .findFirst().orElseThrow()
    label.element.getAttribute("href") == "/projects/p/samples"

    and: "the only link in the panel is that heading plus, at most, the footer"
    links(panel).size() == 1
  }

  def "the footer offers the whole list behind the preview, and is omitted when there is nothing to add"() {
    expect:
    links(SnapshotPanel.samples(progress(3, 3, 0), [], "/s", "All 3 samples \u2192")).size() == 2
    links(SnapshotPanel.samples(progress(3, 3, 0), [], "/s", "")).size() == 1
  }

  /** A failed query must not read as "this project has no datasets": opposite statements. */
  def "a failed dataset query says so instead of rendering an empty state"() {
    when:
    def text = firstRowText(SnapshotPanel.datasets(progress(0, 0, 0), [], [], true, "/d", ""))

    then:
    text == "Datasets could not be loaded"
  }

  /**
   * A panel without a declared flex basis collapses to its content width inside the project page's
   * wrapping row, which left the row half empty. The experiment overview places its panels in a
   * grid, where the basis would fight the grid, so the modifier is opt-in.
   */
  def "the equal-row modifier is opt-in, so a grid-placed panel is not given a flex basis"() {
    expect:
    SnapshotPanel.samples(progress(1, 1, 0), [], "/s", "")
        .getClassNames().contains("snapshot-panel--equal") == false
    SnapshotPanel.samples(progress(1, 1, 0), [], "/s", "").equalInRow()
        .getClassNames().contains("snapshot-panel--equal")
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  private static ExperimentProgress progress(int samples, int measurements, int withData) {
    return new ExperimentProgress("experiment-1", "project-1", "Experiment", samples,
        measurements, measurements, measurements, 0, 0, withData, withData, 0, 0,
        0L, 0L, 0L, 0L)
  }

  private static AssayStanding assay(AssayType type, String method, String instrument,
      int measurements, int withData) {
    return new AssayStanding("experiment-1", "Experiment", type, method, instrument,
        measurements, withData, 0L)
  }

  /** The cells of the table's data rows: header row first, then one row per record. */
  private static List<List<String>> dataRows(com.vaadin.flow.component.Component panel) {
    return rowElements(panel).drop(1).collect { row ->
      row.getChildren().filter { !it.classNames.contains("snapshot-panel-cell-spacer") }
          .map { it.element.text ?: "" }.toList()
    }.toList()
  }

  private static String firstRowText(com.vaadin.flow.component.Component panel) {
    def rows = rowElements(panel).drop(1)
    if (rows.isEmpty()) {
      return null
    }
    return rows.first().getChildren().findFirst().get().element.text
  }

  private static List<com.vaadin.flow.component.Component> rowElements(
      com.vaadin.flow.component.Component panel) {
    return panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-rows") }
        .findFirst()
        .map { it.getChildren().toList() }
        .orElse([])
  }

  private static String metricValue(com.vaadin.flow.component.Component panel) {
    return panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-metric") }
        .findFirst()
        .map { metric -> metric.getChildren()
            .filter { it.classNames.contains("snapshot-panel-count-line") }
            .findFirst().get().getChildren()
            .filter { it.classNames.contains("snapshot-panel-value") }
            .findFirst().get().element.text }
        .orElse(null)
  }

  private static String metricUnit(com.vaadin.flow.component.Component panel) {
    return panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-metric") }
        .findFirst()
        .map { metric -> metric.getChildren()
            .filter { it.classNames.contains("snapshot-panel-count-line") }
            .findFirst().get().getChildren()
            .filter { it.classNames.contains("snapshot-panel-unit") }
            .findFirst().get().element.text }
        .orElse(null)
  }

  private static String qualifier(com.vaadin.flow.component.Component panel) {
    return panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-metric") }
        .findFirst()
        .map { metric -> metric.getChildren()
            .filter { it.classNames.contains("snapshot-panel-qualifier") }
            .findFirst().get().element.text ?: "" }
        .orElse(null)
  }

  private static String stateTag(com.vaadin.flow.component.Component panel) {
    return panel.getChildren()
        .filter { it.classNames.contains("snapshot-panel-head") }
        .findFirst()
        .map { head -> head.getChildren()
            .filter { it.classNames.contains("snapshot-panel-state") }
            .findFirst().orElse(null) }
        .map { it?.element?.text }
        .orElse(null)
  }

  private static List<com.vaadin.flow.component.Component> links(
      com.vaadin.flow.component.Component root) {
    def found = []
    def pending = new ArrayDeque<com.vaadin.flow.component.Component>()
    pending.add(root)
    while (!pending.isEmpty()) {
      def node = pending.poll()
      if (node instanceof com.vaadin.flow.component.html.Anchor) {
        found.add(node)
      }
      pending.addAll(node.getChildren().toList())
    }
    return found
  }

  private static boolean containsClass(com.vaadin.flow.component.Component root, String className) {
    def pending = new ArrayDeque<com.vaadin.flow.component.Component>()
    pending.add(root)
    while (!pending.isEmpty()) {
      def node = pending.poll()
      if (node.classNames.contains(className)) {
        return true
      }
      pending.addAll(node.getChildren().toList())
    }
    return false
  }
}
