package life.qbic.projectmanagement.domain.model.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Analysis method</b>
 *
 * <p>Enumeration of QBiC's analysis method portfolio we offer in collaboration with our partner
 * labs.</p>
 *
 * @since 1.0.0
 */
public enum AnalysisMethod {

  /*
  NGS related analysis methods
   */

  SIXTEEN_S("16S", "16S amplicon sequencing",
      "Amplicon sequencing targeting the V4 region of the 16S rRNA gene"),
  CUSTOM_AMPLICON("CUSTOM-AMPLICON", "Custom amplicon sequencing",
      "Amplicon sequencing from DNA using custom primers"),
  METATRANSCRIPTOMICS(
      "METATRANSCRIPTOMICS", "Metatranscriptomics",
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
  SEQ_ONLY("SEQ-ONLY", "Sequencing only", "Processing of ready-to-sequence pools"),

  /*
  Proteomics related analysis methods
   */

  PROTEOMICS("PROTEOMICS", "Proteomics", ""),

  PHOSPHO_PROTEOMICS("PHOSPHO", "Phosphoproteomics", ""),

  PEPTIDOMICS("PEPTIDOMICS", "Peptidomics", ""),

  INTERACTORS("INTERACTORS", "Interactors", ""),

  PTMS("PTMS", "Posttransductional mutations", ""),

  /*
  Metabolomics related analysis methods
   */

  UNTARGETED_MX("UNTARGETED-MX", "Untargeted metabolomics", ""),

  TARGETED_AA("TARGETED-AA", "Targeted amino acids", ""),

  TARGETED_NUCLEOTIDES("TARGETED-NUCLEOTIDES", "Targeted nucleotides", ""),

  TARGETED_CETO_ACIDS("TARGETED-CETO-ACIDS", "Targeted ceto acids", ""),

  TARGETED_ALL_MX("TARGETED-ALL-MX", "Targeted all metabolites", "");

  private static final Map<String, AnalysisMethod> labelToEnum;

  private static final Map<String, AnalysisMethod> abbreviationToEnum;

  static {
    labelToEnum = new HashMap<>();
    for (AnalysisMethod method : AnalysisMethod.values()) {
      labelToEnum.put(method.label(), method);
    }
  }

  static {
    abbreviationToEnum = new HashMap<>();
    for (AnalysisMethod method : AnalysisMethod.values()) {
      abbreviationToEnum.put(method.abbreviation(), method);
    }
  }

  private final String abbreviation;

  private final String label;

  private final String description;

  AnalysisMethod(String abbreviation, String label, String description) {
    this.abbreviation = abbreviation;
    this.label = label;
    this.description = description;
  }

  public static AnalysisMethod forLabel(String label) throws IllegalArgumentException {
    try {
      return labelToEnum.get(label);
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Unknown label " + label);
    }
  }

  public static Optional<AnalysisMethod> forAbbreviation(String abbreviation) {
    try {
      return Optional.of(abbreviationToEnum.get(abbreviation));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }

  /**
   * Provides the abbreviation for a certain analysis method.
   * <p>
   * Example: WES for whole exome sequencing
   *
   * @return the fixed abbreviation for an analysis method
   * @since 1.0.0
   */
  public String abbreviation() {
    return this.abbreviation;
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
}
