package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
enum MetaDataTypes {
  LIGANDOMICS("Ligandomics", "Detailed Explanation for Ligandomics"), METABOLOMICS("Metabolomics",
      "Detailed Explanation for Metabolomics"), TRANSCRIPTOMIC_GENOMICS("Transciptomics/Genomics",
      "Detailed Explanation for Transcriptomics/Genomics"), PROTEOMICS("Proteomics",
      "Detailed Explanation for Proteomics");
  final String metaDataType;
  final String metaDataDescription;

  MetaDataTypes(String metaDataType, String metaDataDescription) {
    this.metaDataType = metaDataType;
    this.metaDataDescription = metaDataDescription;
  }
}
