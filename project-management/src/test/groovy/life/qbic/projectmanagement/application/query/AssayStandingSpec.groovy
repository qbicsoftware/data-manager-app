package life.qbic.projectmanagement.application.query

import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import spock.lang.Specification

/**
 * Pins the semantics of a primary dataset as the project summary states it.
 *
 * <p>These are the rules that are easy to break silently: a dataset is complete because of the
 * measurements it holds, not because it exists; a count cannot be negative; and the omics domain is
 * a grouping, never the identity. See ADR 0010.</p>
 */
class AssayStandingSpec extends Specification {

  private static AssayStanding standing(int measurements, int withData) {
    return new AssayStanding("experiment-1", "Experiment 1", AssayType.GENOMICS,
        "WES", measurements, withData, 0L)
  }

  def "a dataset with every measurement covered is complete"() {
    expect:
    standing(5, 5).complete()
    standing(5, 5).missingPrimaryData() == 0
  }

  def "a dataset with outstanding measurements is not complete"() {
    expect:
    !standing(272, 264).complete()
    standing(272, 264).missingPrimaryData() == 8
  }

  def "a dataset without measurements is not complete"() {
    // Completeness is a property of data that exists. Reporting "complete" for an empty dataset is
    // the vacuous truth that would make an empty project look finished.
    expect:
    !standing(0, 0).complete()
    standing(0, 0).missingPrimaryData() == 0
  }

  def "missing count never goes negative when the source reports more data than measurements"() {
    // The read model counts distinct measurement records for coverage and distinct
    // (type, id) pairs for totals; a drift between them must not surface as a negative gap.
    expect:
    standing(3, 5).missingPrimaryData() == 0
  }

  def "negative counts are rejected"() {
    when:
    new AssayStanding("experiment-1", "Experiment 1", AssayType.GENOMICS, "WES", -1, 0, 0L)

    then:
    thrown(IllegalArgumentException)
  }

  def "two datasets of the same experiment and method are distinguished only by the experiment"() {
    given:
    def first = new AssayStanding("experiment-1", "Experiment 1", AssayType.GENOMICS,
        "WES", 4, 4, 100L)
    def second = new AssayStanding("experiment-2", "Experiment 2", AssayType.GENOMICS,
        "WES", 6, 0, 0L)

    expect:
    // Same method, two experiments, two datasets -- which is why a count per method cannot be
    // derived from the method alone.
    first != second
    first.methodLabel() == second.methodLabel()
    first.experimentId() != second.experimentId()
  }

  def "the omics domain groups analysis methods but is not the identity"() {
    expect:
    AssayType.of(AnalysisMethod.WES) == AssayType.GENOMICS
    AssayType.of(AnalysisMethod.RNA_SEQ) == AssayType.GENOMICS
    AssayType.of(AnalysisMethod.SIXTEEN_S) == AssayType.GENOMICS
    AssayType.of(AnalysisMethod.WES) == AssayType.of(AnalysisMethod.RNA_SEQ)

    and: "WES and RNA-Seq are the same domain but different datasets"
    AnalysisMethod.WES != AnalysisMethod.RNA_SEQ
  }

  def "every analysis method maps to exactly one domain"() {
    expect:
    AnalysisMethod.values().every { method -> AssayType.of(method) != null }
  }

  def "the domain of a dataset comes from the measurement, not from the analysis method"() {
    expect:
    standing(1, 1).assayType() == AssayType.GENOMICS
  }

  def "proteomics and immunopeptidomics are different datasets despite the same analysis method"() {
    // The one thing that cannot be inferred from the analysis method. Proteomics and
    // immunopeptidomics samples carry the SAME analysis method; what distinguishes them is what the
    // measurement records. An earlier read model derived the domain from the analysis method and
    // reported every immunopeptidomics dataset as proteomics.
    given:
    def proteomics = new AssayStanding("e1", "Experiment 1", AssayType.PROTEOMICS,
        "CIDOT", 6, 6, 0L)
    def immunopeptidomics = new AssayStanding("e1", "Experiment 1", AssayType.IMMUNOPEPTIDOMICS,
        "HCDOT", 8, 8, 0L)

    expect:
    proteomics.assayType() != immunopeptidomics.assayType()
    proteomics != immunopeptidomics
  }

  def "two datasets of the same method and domain are still distinct rows"() {
    given:
    def first = new AssayStanding("e1", "Experiment 1", AssayType.GENOMICS,
        "RNA-SEQ", 3, 1, 0L)
    def second = new AssayStanding("e1", "Experiment 1", AssayType.GENOMICS,
        "RNA-SEQ", 2, 2, 0L)

    expect: "same experiment, domain and method; only the counts differ"
    first.assayType() == second.assayType()
    first.methodLabel() == second.methodLabel()
    first.measurements() != second.measurements()
  }
}
