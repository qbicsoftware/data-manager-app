package life.qbic.projectmanagement.application.query;

/**
 * <b>Assay type</b>
 *
 * <p>The technology class of a measurement, as the read models count it: genomics (NGS),
 * proteomics (PXP) and immunopeptidomics (IP). Every per-type figure in the project read models --
 * measurement counts, measurements carrying primary data, primary data volume -- is addressed by
 * this enum, so no consumer has to re-derive the NGS/PXP/IP triple.</p>
 *
 * <p>It carries no display label on purpose. Naming is the view layer's business, and that layer
 * already owns {@code life.qbic.datamanager.views.MeasurementType}, which holds both the label and
 * the tag colour. This enum only says which column to read, so the application layer never depends
 * on the UI for a word.</p>
 *
 * <p>It is deliberately <em>not</em> an analysis type such as RNA-Seq or 16S. One extract can
 * legitimately yield both, so the analysis method belongs on the measurement and must not be folded
 * into this vocabulary. The analysis method is a separate axis, carried by
 * {@link life.qbic.projectmanagement.domain.model.sample.AnalysisMethod} and grouped into these
 * domains by {@link #of(life.qbic.projectmanagement.domain.model.sample.AnalysisMethod)}: WES and
 * RNA-Seq are both genomics, but they are different datasets and readers pick between them, so the
 * domain is a grouping and never the identity.</p>
 *
 * @see ExperimentProgress
 * @see AssayStanding
 * @since 1.19.0
 */
public enum AssayType {
  GENOMICS,
  PROTEOMICS,
  IMMUNOPEPTIDOMICS;

  /**
   * Resolves an assay type from the string a read model stores in its {@code measurement_type}
   * column.
   *
   * <p><b>Two conventions exist in this codebase.</b> {@code MeasurementDataQuery} writes the enum
   * names ({@code GENOMICS}, {@code PROTEOMICS}, {@code IMMUNOPEPTIDOMICS}), while
   * {@code v_measurement_experiment} and {@code v_project_assay_standing} write the short table codes
   * ({@code NGS}, {@code PXP}, {@code IP}). {@link #valueOf(String)} accepts only the first, so
   * reading the second threw {@code No enum constant AssayType.IP} on every row -- which surfaced as
   * a project page that silently showed no datasets.</p>
   *
   * @param value the column value, in either convention
   * @throws IllegalArgumentException if the value matches neither
   */
  public static AssayType fromColumnValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("measurement_type must not be null");
    }
    return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "NGS", "GENOMICS" -> GENOMICS;
      case "PXP", "PROTEOMICS" -> PROTEOMICS;
      case "IP", "IMMUNOPEPTIDOMICS" -> IMMUNOPEPTIDOMICS;
      default -> throw new IllegalArgumentException(
          "Unknown measurement_type: " + value);
    };
  }

  /**
   * The domain an analysis method belongs to.
   *
   * <p>Every method maps to exactly one domain. Metabolomics methods are grouped with proteomics
   * because the read models carry no metabolomics bucket and the domain is only used for grouping,
   * never for counting: a metabolomics dataset stays individually addressable by its analysis
   * method, so grouping it here cannot merge it with a proteomics dataset.</p>
   */
  public static AssayType of(
      life.qbic.projectmanagement.domain.model.sample.AnalysisMethod analysisMethod) {
    return switch (analysisMethod) {
      // NGS-related analysis methods
      case SIXTEEN_S, CUSTOM_AMPLICON, METATRANSCRIPTOMICS, METAGENOMIC, WGS, WES, ATAC_SEQ,
           RNA_SEQ, SC_ATAC_SEQ, SC_RNA_SEQ, SC_AMPLICON_SEQ, ONT_METAGENOMIC, ONT_WGS, ONT_RNA,
           ONT_APMLICON, PACBIO_HIFI, PACBIO_ISOSEQ, ISOLATION_ONLY, QC_ONLY, SEQ_ONLY ->
          GENOMICS;
      // Proteomics-related analysis methods, and the metabolomics family that shares the bucket
      case PROTEOMICS, PHOSPHO_PROTEOMICS, PEPTIDOMICS, INTERACTORS, PTMS, UNTARGETED_MX,
           TARGETED_AA, TARGETED_NUCLEOTIDES, TARGETED_CETO_ACIDS, TARGETED_ALL_MX ->
          PROTEOMICS;
    };
  }
}
