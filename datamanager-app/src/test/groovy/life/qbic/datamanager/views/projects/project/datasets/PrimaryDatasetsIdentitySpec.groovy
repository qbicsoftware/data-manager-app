package life.qbic.datamanager.views.projects.project.datasets

import life.qbic.projectmanagement.application.query.AssayStanding
import life.qbic.projectmanagement.application.query.AssayType
import spock.lang.Specification

/**
 * Covers the identity the project's Datasets table distinguishes.
 *
 * <p>A primary dataset is one experiment for one omics domain, method and <em>instrument</em>. The
 * table used to omit the instrument, so two datasets differing only in instrument rendered
 * identically -- same experiment, same method, same size -- and the table appeared to have printed
 * one row twice. These specs pin that each dataset the read model emits gets its own distinguishable
 * row, and that a row addresses its own dataset rather than merely its experiment.</p>
 */
class PrimaryDatasetsIdentitySpec extends Specification {

  /**
   * The reported defect: two proteomics datasets of one experiment on different instruments were
   * indistinguishable on screen.
   */
  def "two datasets that differ only by instrument are two rows with different instruments"() {
    given:
    def datasets = [
        assay("Bite Trial", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Exploris 480", 1, 1),
        assay("Bite Trial", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Fusion Lumos", 5, 1),
    ]

    when:
    def rows = PrimaryDatasetsComponent.datasetRows(datasets)

    then: "both datasets are listed"
    rows.size() == 2

    and: "and they are told apart by the instrument, which is part of their identity"
    rows*.instrumentLabel().toSet() ==
        ["Orbitrap Exploris 480", "Orbitrap Fusion Lumos"] as Set

    and: "every other field they share is genuinely shared, which is why the instrument is required"
    rows*.experimentName().toSet() == ["Bite Trial"] as Set
    rows*.methodLabel().toSet() == ["PROTEOMICS"] as Set
  }

  /**
   * A measurement that records no instrument has none. The column must say so rather than leaving a
   * blank that reads as a rendering fault -- and must not substitute the method, which is how the
   * read model's two independent columns were once conflated.
   */
  def "a dataset without a recorded instrument keeps a null instrument rather than borrowing one"() {
    given:
    def datasets = [assay("Awesome", AssayType.GENOMICS, "WGS", null, 2, 0)]

    when:
    def row = PrimaryDatasetsComponent.datasetRows(datasets).first()

    then:
    row.instrumentLabel() == null
    row.methodLabel() == "WGS"
  }

  def "rows are ordered by experiment, then domain, method and instrument"() {
    given:
    def datasets = [
        assay("zeta", AssayType.GENOMICS, "WGS", "Illumina", 1, 1),
        assay("Alpha", AssayType.GENOMICS, "WES", "Illumina", 1, 1),
        assay("Alpha", AssayType.GENOMICS, "WGS", "Illumina NovaSeq", 1, 1),
        assay("Alpha", AssayType.GENOMICS, "WGS", "Illumina", 1, 1),
    ]

    when:
    def rows = PrimaryDatasetsComponent.datasetRows(datasets)

    then: "experiment first, case-insensitively, so names sort the way a reader expects"
    rows*.experimentName() == ["Alpha", "Alpha", "Alpha", "zeta"]

    and: "within one experiment, method then instrument"
    rows.take(3)*.methodLabel() == ["WES", "WGS", "WGS"]
    rows.drop(1).take(2)*.instrumentLabel() == ["Illumina", "Illumina NovaSeq"]
  }

  /**
   * A row must address its own dataset, not merely its experiment. Two datasets of one experiment
   * differ only by method and instrument, so linking to the experiment alone dropped the reader on
   * a page whose default selection was one of them -- for the clicked row, possibly the other one.
   */
  def "each row's destination identifies its own dataset, not just its experiment"() {
    given: "two datasets of one experiment that differ only by instrument"
    def datasets = [
        assay("Bite Trial", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Exploris 480", 1, 1),
        assay("Bite Trial", AssayType.PROTEOMICS, "PROTEOMICS", "Orbitrap Fusion Lumos", 5, 1),
    ]

    when:
    def rows = PrimaryDatasetsComponent.datasetRows(datasets)
    def urls = rows.collect { row ->
      life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetSegment.of(
          row.assayType(), row.methodLabel(), row.instrumentLabel()).urlValue()
    }

    then: "the two rows resolve to two different dataset selections"
    urls.toSet().size() == 2

    and: "each names its domain, method and instrument, which is the page's own identity form"
    urls == [
        "PROTEOMICS:PROTEOMICS:Orbitrap Exploris 480",
        "PROTEOMICS:PROTEOMICS:Orbitrap Fusion Lumos",
    ]
  }

  /**
   * The domain comes from the measurement, never from the method: proteomics and immunopeptidomics
   * share analysis-method vocabulary, so deriving one from the other mislabels datasets.
   */
  def "the domain is carried as its own field and is never derived from the method"() {
    given: "the same method label under two different domains"
    def datasets = [
        assay("e1", AssayType.PROTEOMICS, "PEPTIDOMICS", "instrument", 1, 1),
        assay("e1", AssayType.IMMUNOPEPTIDOMICS, "PEPTIDOMICS", "instrument", 1, 1),
    ]

    when:
    def rows = PrimaryDatasetsComponent.datasetRows(datasets)

    then: "they stay two datasets, on their own domains"
    rows*.assayType() == [AssayType.PROTEOMICS, AssayType.IMMUNOPEPTIDOMICS]
  }

  def "coverage is derived per dataset, not shared across the project"() {
    given:
    def datasets = [
        assay("e1", AssayType.GENOMICS, "WGS", "Illumina", 2, 2),
        assay("e1", AssayType.GENOMICS, "WES", "Illumina", 5, 1),
        assay("e1", AssayType.GENOMICS, "RNA-SEQ", "Illumina", 1, 0),
    ]

    when:
    def rows = PrimaryDatasetsComponent.datasetRows(datasets)

    then: "each dataset reports its own gap, in method order (RNA-SEQ, WES, WGS)"
    rows*.methodLabel() == ["RNA-SEQ", "WES", "WGS"]
    rows*.missing() == [1, 4, 0]
  }

  /**
   * The table is ONE grid now, and every row is a set of cells in it. That only works if the header
   * and the data rows emit the same number of cells: a short row does not fail loudly, it packs its
   * cells into the wrong tracks and the columns drift apart -- which is exactly how the header ended
   * up over the wrong values.
   */
  def "the header and a data row emit the same number of cells"() {
    given: "a component whose collaborators are never called by the row builder"
    def component = new PrimaryDatasetsComponent(
        Mock(life.qbic.projectmanagement.application.api.ProjectQueryService),
        "", Mock(life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory))
    def row = PrimaryDatasetsComponent.datasetRows([
        assay("e1", AssayType.GENOMICS, "WGS", "Illumina", 2, 1)]).first()

    expect:
    PrimaryDatasetsComponent.headerRow().getChildren().count()
        == component.datasetRow(row).getChildren().count()

    and: "the column list covers the identity, the figures and the action"
    PrimaryDatasetsComponent.HEADERS.length == 9
  }

  // ── The scale row ──────────────────────────────────────────────────────

  /**
   * The row used to place one dataset figure beside three measurement figures with nothing naming
   * the grain, so "17 missing data" read as seventeen DATASETS missing data -- contradicting the
   * "3 datasets" next to it.
   */
  def "the scale row groups its figures by the grain they count"() {
    given:
    def rows = PrimaryDatasetsComponent.datasetRows([
        assay("e1", AssayType.GENOMICS, "WGS", "Illumina", 6, 1),
        assay("e1", AssayType.PROTEOMICS, "WGS", "Illumina", 6, 1),
        assay("e1", AssayType.IMMUNOPEPTIDOMICS, "WGS", "Illumina", 8, 1),
    ])

    when:
    def rollup = PrimaryDatasetsComponent.rollup(rows)

    then: "two groups, each naming what it counts"
    groups(rollup)*.get(0) == ["Datasets", "Measurements"]

    and: "the dataset group holds one figure, the measurement group holds three"
    figuresByGroup(rollup)["Datasets"] == ["3"]
    figuresByGroup(rollup)["Measurements"] == ["20 total", "3 with data", "17 missing"]

    and: "the lone dataset figure carries no qualifier, because its group label already names it"
    !figuresByGroup(rollup)["Datasets"].first().contains(" ")
  }

  /**
   * A reader who adds up the measurement total and compares it to a measurement list elsewhere must
   * not conclude the page is wrong: a measurement spanning several analysis methods is counted once
   * per dataset it belongs to (ADR 0010).
   */
  def "the measurement total explains that it counts attributions"() {
    given:
    def rows = PrimaryDatasetsComponent.datasetRows([
        assay("e1", AssayType.GENOMICS, "WGS", "Illumina", 2, 2)])

    when:
    def rollup = PrimaryDatasetsComponent.rollup(rows)

    then: "only the total carries the caveat; it would be noise on the coverage figures"
    describedFigures(rollup) == ["2 total"]
  }

  private static List<List<String>> groups(com.vaadin.flow.component.Component rollup) {
    return rollup.getChildren().toList().collect { group ->
      def children = group.getChildren().toList()
      def label = children.find {
        it.getClassNames().contains("project-datasets-rollup-group-label")
      }.element.text
      return [label]
    }
  }

  private static Map<String, List<String>> figuresByGroup(
      com.vaadin.flow.component.Component rollup) {
    def result = [:]
    rollup.getChildren().toList().each { group ->
      def children = group.getChildren().toList()
      def label = children.find {
        it.getClassNames().contains("project-datasets-rollup-group-label")
      }.element.text
      def stats = children.find {
        it.getClassNames().contains("project-datasets-rollup-stats")
      }
      result[label] = stats.getChildren().toList().collect { stat ->
        def parts = stat.getChildren().toList()
        parts.collect { it.element.text }.join(" ")
      }
    }
    return result
  }

  private static List<String> describedFigures(com.vaadin.flow.component.Component rollup) {
    def found = []
    rollup.getChildren().toList().each { group ->
      group.getChildren().toList().each { child ->
        child.getChildren().toList().each { stat ->
          if (stat.getClassNames().contains("project-datasets-stat--described")) {
            found.add(stat.getChildren().toList().collect { it.element.text }.join(" "))
          }
        }
      }
    }
    return found
  }

  private static AssayStanding assay(String experiment, AssayType type, String method,
      String instrument, int measurements, int withData) {
    return new AssayStanding("experiment-" + experiment, experiment, type, method, instrument,
        measurements, withData, 0L)
  }
}
