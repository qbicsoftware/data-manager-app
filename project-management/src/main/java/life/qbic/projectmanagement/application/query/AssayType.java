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
 * into this vocabulary.</p>
 *
 * @see ExperimentProgress
 * @since 1.19.0
 */
public enum AssayType {
  GENOMICS,
  PROTEOMICS,
  IMMUNOPEPTIDOMICS
}
