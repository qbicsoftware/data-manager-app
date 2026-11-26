package life.qbic.projectmanagement.application.query

import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Covers the two derived views of an omics domain.
 *
 * <p>{@code fromColumnValue} reads the {@code measurement_type} column, and the codebase writes it
 * in two conventions: {@code MeasurementDataQuery} writes enum names, while
 * {@code v_measurement_experiment} and {@code v_project_assay_standing} write the short table codes.
 * {@code valueOf} accepts only the first, so reading the second threw
 * {@code No enum constant AssayType.IP} on every row — which surfaced as a project page that
 * silently showed no datasets. That failure mode is why both conventions are pinned here.</p>
 *
 * <p>{@code of} groups an analysis method into its domain. The domain is a grouping and never the
 * dataset identity, so what matters is that every method maps to exactly one domain and that two
 * methods of one domain stay distinguishable by their own label.</p>
 */
class AssayTypeSpec extends Specification {

  // ── fromColumnValue: both column conventions ───────────────────────────

  @Unroll
  def "reads the short table code '#value' as #expected"() {
    expect:
    AssayType.fromColumnValue(value) == expected

    where:
    value     | expected
    "NGS"     | AssayType.GENOMICS
    "PXP"     | AssayType.PROTEOMICS
    "IP"      | AssayType.IMMUNOPEPTIDOMICS
  }

  @Unroll
  def "reads the enum name '#value' as #expected"() {
    expect:
    AssayType.fromColumnValue(value) == expected

    where:
    value                | expected
    "GENOMICS"           | AssayType.GENOMICS
    "PROTEOMICS"         | AssayType.PROTEOMICS
    "IMMUNOPEPTIDOMICS"  | AssayType.IMMUNOPEPTIDOMICS
  }

  @Unroll
  def "the column read is case- and whitespace-insensitive for '#value'"() {
    expect:
    AssayType.fromColumnValue(value) == expected

    where:
    value          | expected
    "ngs"          | AssayType.GENOMICS
    " Genomics "   | AssayType.GENOMICS
    "\tpxp\n"      | AssayType.PROTEOMICS
    "Ip"           | AssayType.IMMUNOPEPTIDOMICS
  }

  /**
   * A null column means the query produced no value, which is a defect in the read model rather
   * than a dataset without a domain. Failing loudly beats resolving to a default domain and
   * mislabelling every row it touches.
   */
  def "a null column value fails rather than defaulting to a domain"() {
    when:
    AssayType.fromColumnValue(null)

    then:
    thrown(IllegalArgumentException)
  }

  def "an unknown column value fails with the offending value named"() {
    when:
    AssayType.fromColumnValue("METABOLOMICS")

    then:
    def error = thrown(IllegalArgumentException)
    error.message.contains("METABOLOMICS")
  }

  // ── of: every analysis method has exactly one domain ───────────────────

  /**
   * Exhaustive on purpose: a new analysis method that nobody classified would otherwise throw at
   * runtime inside a page render, which is how the missing-domain bug would reach production.
   */
  def "every analysis method maps to a domain"() {
    expect:
    AnalysisMethod.values().every { method ->
      AssayType.of(method) != null
    }
  }

  @Unroll
  def "the genomics methods group into GENOMICS (#method)"() {
    expect:
    AssayType.of(method) == AssayType.GENOMICS

    where:
    method << genomicsMethods()
  }

  @Unroll
  def "the proteomics and metabolomics methods group into PROTEOMICS (#method)"() {
    expect:
    AssayType.of(method) == AssayType.PROTEOMICS

    where:
    method << proteomicsMethods()
  }

  /**
   * The grouping is not the identity: two methods of one domain must stay distinguishable, because
   * WES and RNA-Seq are both genomics and a reader picks between them. This is the property that
   * makes the domain safe to use as a grouping only.
   */
  def "two methods of one domain remain distinct values"() {
    given:
    def wes = AnalysisMethod.WES
    def rnaSeq = AnalysisMethod.RNA_SEQ

    expect:
    AssayType.of(wes) == AssayType.of(rnaSeq)
    wes.abbreviation() != rnaSeq.abbreviation()
  }

  private static List<AnalysisMethod> genomicsMethods() {
    return [
        AnalysisMethod.SIXTEEN_S, AnalysisMethod.CUSTOM_AMPLICON, AnalysisMethod.METATRANSCRIPTOMICS,
        AnalysisMethod.METAGENOMIC, AnalysisMethod.WGS, AnalysisMethod.WES, AnalysisMethod.ATAC_SEQ,
        AnalysisMethod.RNA_SEQ, AnalysisMethod.SC_ATAC_SEQ, AnalysisMethod.SC_RNA_SEQ,
        AnalysisMethod.SC_AMPLICON_SEQ, AnalysisMethod.ONT_METAGENOMIC, AnalysisMethod.ONT_WGS,
        AnalysisMethod.ONT_RNA, AnalysisMethod.ONT_APMLICON, AnalysisMethod.PACBIO_HIFI,
        AnalysisMethod.PACBIO_ISOSEQ, AnalysisMethod.ISOLATION_ONLY, AnalysisMethod.QC_ONLY,
        AnalysisMethod.SEQ_ONLY,
    ]
  }

  private static List<AnalysisMethod> proteomicsMethods() {
    return [
        AnalysisMethod.PROTEOMICS,
        AnalysisMethod.PHOSPHO_PROTEOMICS,
        AnalysisMethod.PEPTIDOMICS,
        AnalysisMethod.INTERACTORS, AnalysisMethod.PTMS,
        AnalysisMethod.UNTARGETED_MX,
        AnalysisMethod.TARGETED_AA, AnalysisMethod.TARGETED_NUCLEOTIDES,
        AnalysisMethod.TARGETED_CETO_ACIDS, AnalysisMethod.TARGETED_ALL_MX,
    ]
  }
}
