package life.qbic.datamanager.views.projects.project.samples.registration.batch;


/**
 * MetadataType enums are used in {@link BatchInformationLayout}, to indicate which type of Sample
 * Metadata will be provided during Sample Registration. Additionally, they host a detailed
 * description for the respective metadata type
 *
 * @since 1.0.0
 */
enum MetadataType {
  LIGANDOMICS("Ligandomics", "Detailed Explanation for Ligandomics"), METABOLOMICS("Metabolomics",
      "Detailed Explanation for Metabolomics"), TRANSCRIPTOMICS_GENOMICS("Transcriptomics/Genomics",
      "Detailed Explanation for Transcriptomics/Genomics"), PROTEOMICS("Proteomics",
      "Detailed Explanation for Proteomics");
  final String label;
  final String description;

  MetadataType(String label, String description) {
    this.label = label;
    this.description = description;
  }
}
