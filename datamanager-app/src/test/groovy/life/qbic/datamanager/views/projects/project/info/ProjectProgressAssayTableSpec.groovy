package life.qbic.datamanager.views.projects.project.info

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import life.qbic.projectmanagement.application.query.AssayStanding
import life.qbic.projectmanagement.application.query.AssayType
import spock.lang.Specification

/**
 * Covers the "Measurement datasets" card of the project page's progress band.
 *
 * <p>The card used to render one uppercase caption per omics domain, each governing a single row.
 * That spent six lines on three facts, set the field that varies (the domain) as the faintest thing
 * on the card, and left the right-hand numbers across a void from the method with no header saying
 * what they measured. It is now one table with a header row — the same construction the experiment
 * panels use — so it scales to the dozen assays a multi-omics project runs.</p>
 *
 * <p>Asserts {@code assayTable} directly: it reads only its argument, and building the section would
 * need collaborators this classpath cannot mock.</p>
 */
class ProjectProgressAssayTableSpec extends Specification {

  def "the table renders a header row and one row per dataset"() {
    given:
    def datasets = [
        assay(AssayType.GENOMICS, "WGS", 1, 6, 1),
        assay(AssayType.PROTEOMICS, "WGS", 1, 6, 1),
    ]

    when:
    def table = tableOf(datasets)

    then:
    def rows = tableRows(table)
    rows.size() == 3

    and: "the header names the base of each number, so a dataset count cannot be read as a measurement count"
    rows[0] == ["Omics domain", "Method", "Datasets", "Measurements with data", "Availability"]

    and: "each dataset is one row, with the domain as a column rather than a caption"
    rows[1] == ["Genomics", "WGS", "1", "1 of 6", "Partial"]
    rows[2] == ["Proteomics", "WGS", "1", "1 of 6", "Partial"]
  }

  /**
   * The header emits the spacer too. Skipping it for the header put its five labels into the first
   * five tracks of a six-track grid, so the header sat one column off against every row beneath it.
   */
  def "the header row emits the spacer cell, so it aligns with the data rows"() {
    when:
    def table = tableOf([assay(AssayType.GENOMICS, "WGS", 1, 6, 1)])

    then: "header and data rows agree on the number of cells including the spacer"
    cellCountIncludingSpacer(table, 0) == 6
    cellCountIncludingSpacer(table, 1) == 6
  }

  def "the grid declares as many tracks as a row emits cells"() {
    when:
    def table = tableOf([assay(AssayType.GENOMICS, "WGS", 1, 6, 1)])

    then: "five data cells plus one spacer track"
    table.classNames.contains("project-progress-assays--5-columns")
  }

  /** The caption-per-domain layout made one row per domain normal; a dozen assays is the real case. */
  def "the table holds many datasets without turning into a stack of captions"() {
    given: "five genomics assays plus one of each other domain"
    def datasets = [
        assay(AssayType.GENOMICS, "WGS", 2, 12, 4),
        assay(AssayType.GENOMICS, "WES", 1, 6, 0),
        assay(AssayType.GENOMICS, "RNA-Seq", 1, 4, 4),
        assay(AssayType.PROTEOMICS, "CIDOT", 1, 8, 8),
        assay(AssayType.IMMUNOPEPTIDOMICS, "CIDOT", 1, 3, 0),
    ]

    when:
    def table = tableOf(datasets)

    then: "every dataset gets a row up to the cap, and nothing renders a per-domain caption"
    tableRows(table).size() == 5
    !containsClass(table, "project-progress-assays-domain")

    and: "the rows stay in domain then method order, grouped by domain rather than interleaved"
    tableRows(table).drop(1)*.get(0) ==
        ["Genomics", "Genomics", "Genomics", "Proteomics"]
  }

  def "state uses the shared vocabulary as a pill, and colour varies with the state"() {
    given:
    def datasets = [
        assay(AssayType.GENOMICS, "WGS", 1, 6, 6),
        assay(AssayType.PROTEOMICS, "WGS", 1, 6, 3),
        assay(AssayType.IMMUNOPEPTIDOMICS, "WGS", 1, 6, 0),
    ]

    when:
    def table = tableOf(datasets)

    then: "one word per state, from DataState, not a second dialect"
    tableRows(table).drop(1)*.get(4) == ["Available", "Partial", "Waiting"]

    and: "each is rendered as the shared snapshot-panel pill, toned by its state"
    pillTones(table) == ["snapshot-panel-state--done",
                         "snapshot-panel-state--partial",
                         "snapshot-panel-state--waiting"]
  }

  def "a dataset with no measurements at all says none rather than 0 of 0"() {
    when:
    def table = tableOf([assay(AssayType.GENOMICS, "WGS", 1, 0, 0)])

    then:
    tableRows(table)[1][3] == "none"
    tableRows(table)[1][4] == "Waiting"
  }

  /** Rows are capped, so the table cannot grow past the rail it sits under. */
  def "the table is capped at four dataset rows"() {
    given:
    def datasets = (1..6).collect { assay(AssayType.GENOMICS, "M${it}", 1, 1, 0) }

    when:
    def table = tableOf(datasets)

    then: "header plus four rows"
    tableRows(table).size() == 5
  }

  // ── Helpers ────────────────────────────────────────────────────────────

  /** The rolled-up rows in display order, then rendered as the card renders them. */
  private static Div tableOf(List<AssayStanding> datasets) {
    return ProjectProgressSection.assayTable(ProjectProgressSection.assayRollup(datasets))
  }

  private static AssayStanding assay(AssayType type, String method, int datasets,
      int measurements, int withData) {
    return new AssayStanding("experiment-1", "Experiment", type, method, null,
        measurements, withData, 0L)
  }

  /** The rows, each as its list of cell texts with the spacer removed. */
  private static List<List<String>> tableRows(Div table) {
    return table.getChildren()
        .filter { it.classNames.contains("project-progress-assay-row") }
        .collect { row -> row.getChildren()
            .filter { !it.classNames.contains("project-progress-assay-spacer") }
            .collect { (it as Span).text }
            .toList() }
        .toList()
  }

  private static int cellCountIncludingSpacer(Div table, int rowIndex) {
    return table.getChildren()
        .filter { it.classNames.contains("project-progress-assay-row") }
        .toList()[rowIndex]
        .getChildren().size()
  }

  private static List<String> pillTones(Div table) {
    def tones = []
    table.getChildren()
        .filter { it.classNames.contains("project-progress-assay-row") }
        .toList()
        .each { row ->
          [row.getChildren().toList()].flatten()
              .findAll { it.classNames.contains("snapshot-panel-state") }
              .each { pill ->
                tones.add(pill.classNames.find { name -> name.startsWith("snapshot-panel-state--") })
              }
        }
    return tones
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
