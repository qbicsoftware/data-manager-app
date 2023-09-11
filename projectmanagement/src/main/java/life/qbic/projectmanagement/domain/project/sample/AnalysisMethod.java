package life.qbic.projectmanagement.domain.project.sample;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public enum AnalysisMethod {

  SIXTEEN_S("16S", "16S amplicon sequencing",
      "Amplicon sequencing targeting the V4 region of the 16S rRNA gene"),

  CUSTOM_AMPLICON("CUSTOM-AMPLICON", "Custom amplicon sequencing",
      "Amplicon sequencing from DNA using custom primers"),
  METATRANSCRIPTOMICS("METATRANSCRIPTOMICS", "Metatrascriptomics",
      "Sequencing of the total RNA content in a community present in a sample"),
  METAGENOMIC("METAGENOMIC", "Metagenomics",
      "Sequencing of the entire genetic content in a community present in a sample"),
  WGS("WGS", "Genome sequencing", "Sequencing of the entire genome of an organism"),
  WES("WES", "Exome sequencing", "Sequencing of all exons of protein-coding genes of an organism"),

  ATAC_SEQ("ATAC-SEQ", "ATAC sequencing",
      "Assay for transposase-accessible chromatin with sequencing"),

  RNA_SEQ("RNA-SEQ", "RNA sequencing", "Detection and quantitative analysis of RNA in a sample"),

  SC_ATAC_SEQ("SC-ATAC-SEQ", "Single-cell ATAC sequencing",
      "Assay for transposase-accessible chromatin with sequencing at single-cell resolution"),

  SC_RNA_SEQ("SC-RNA-SEQ", "Single-cell RNA sequencing",
      "RNA sequencing at single-cell resolution"),

  SC_AMPLICON_SQE("SC-AMPLICON-SEQ", "Single-cell amplicon sequencing",
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

  private final String fixedTerm;

  private final String label;

  private final String description;

  AnalysisMethod(String fixedTerm, String label, String description) {
    this.fixedTerm = fixedTerm;
    this.label = label;
    this.description = description;
  }

  public String term() {
    return this.fixedTerm;
  }

  public String label() {
    return this.label;
  }

  public String description() {
    return this.description;
  }

}
