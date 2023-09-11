package life.qbic.projectmanagement.domain.project.sample;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>Analysis method</b>
 *
 * <p>Enumeration of QBiC's analysis method portfolio we offer in collaboration with our partner
 * labs.</p>
 *
 * @since 1.0.0
 */
public enum AnalysisMethod {


  SIXTEEN_S("16S", "16S amplicon sequencing",
      "Amplicon sequencing targeting the V4 region of the 16S rRNA gene"),
  CUSTOM_AMPLICON("CUSTOM-AMPLICON", "Custom amplicon sequencing",
      "Amplicon sequencing from DNA using custom primers"),
  METATRANSCRIPTOMICS(
      "METATRANSCRIPTOMICS", "Metatrascriptomics",
      "Sequencing of the total RNA content in a community present in a sample"),
  METAGENOMIC(
      "METAGENOMIC", "Metagenomics",
      "Sequencing of the entire genetic content in a community present in a sample"),
  WGS("WGS",
      "Genome sequencing", "Sequencing of the entire genome of an organism"),
  WES("WES",
      "Exome sequencing", "Sequencing of all exons of protein-coding genes of an organism"),
  ATAC_SEQ("ATAC-SEQ", "ATAC sequencing",
      "Assay for transposase-accessible chromatin with sequencing"),
  RNA_SEQ("RNA-SEQ", "RNA sequencing", "Detection and quantitative analysis of RNA in a sample"),
  SC_ATAC_SEQ("SC-ATAC-SEQ", "Single-cell ATAC sequencing",
      "Assay for transposase-accessible chromatin with sequencing at single-cell resolution"),
  SC_RNA_SEQ("SC-RNA-SEQ", "Single-cell RNA sequencing",
      "RNA sequencing at single-cell resolution"),
  SC_AMPLICON_SEQ("SC-AMPLICON-SEQ", "Single-cell amplicon sequencing",
      "Amplicon sequencing at single-cell resolution"),
  ONT_METAGENOMIC("ONT-METAGENOMIC", "Nanopore metagenomics",
      "Sequencing of the entire genetic content in a community present in a sample with Nanopore technology"),
  ONT_WGS("ONT-WGS", "Nanopore genome sequencing",
      "Sequencing of the entire genome of an organism with Nanopore technology"),
  ONT_RNA("ONT-RNA", "Nanopore RNA sequencing",
      "Detection and quantitative analysis of RNA in a sample with Nanopore technology"),
  ONT_APMLICON("ONT-AMPLICON", "Nanopore amplicon sequencing",
      "Amplicon sequencing with Nanopore technology"),
  PACBIO_HIFI("PACBIO-HIFI", "PacBio HiFi",
      "Sequencing of the entire genome of an organism with PacBio technology"),
  PACBIO_ISOSEQ("PACBIO-ISOSEQ", "PacBio IsoSeq",
      "Detection and quantitative analysis of RNA in a sample with PacBio technology"),
  ISOLATION_ONLY("ISOLATION-ONLY", "Isolation only", "DNA and RNA isolation only, no sequencing"),
  QC_ONLY("QC-ONLY", "QC only", "Quality control only, no sequencing"),
  SEQ_ONLY("SEQ-ONLY", "Sequencing only", "Processing of ready-to-sequence pools");

  private static final Map<String, AnalysisMethod> fixedTermToEnum;

  static {
    fixedTermToEnum = new HashMap<>();
    for(AnalysisMethod method : AnalysisMethod.values()) {
      fixedTermToEnum.put(method.term(), method);
    }
  };


  private final String fixedTerm;

  private final String label;

  private final String description;

  AnalysisMethod(String fixedTerm, String label, String description) {
    this.fixedTerm = fixedTerm;
    this.label = label;
    this.description = description;
  }

  /**
   * Provides the fixed term for a certain analysis method.
   * <p>
   * Example: WES for whole exome sequencing
   *
   * @return the fixed term for an analysis method
   * @since 1.0.0
   */
  public String term() {
    return this.fixedTerm;
  }

  /**
   * Provides a short label of the analysis method.
   * <p>
   * Example: WES -> Whole exome sequencing
   *
   * @return the short label of the analysis method
   * @since 1.0.0
   */
  public String label() {
    return this.label;
  }

  /**
   * Provides a short description about the analysis method.
   *
   * @return written description
   * @since 1.0.0
   */
  public String description() {
    return this.description;
  }

  public static AnalysisMethod forFixedTerm(String term) throws IllegalArgumentException {
    try {
      return fixedTermToEnum.get(term);
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Unknown term " + term);
    }
  }
}
