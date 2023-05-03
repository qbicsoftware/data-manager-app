package life.qbic.datamanager.views.projects.project.samples.batchRegistration;


/**
 * MetaDataType enums are used in {@link GeneralInformationLayout}, to indicate which type of Sample
 * Metadata will be provided during Sample Registration. Additionally, they host a detailed
 * description for the relative metadatatype
 *
 * @since 1.0.0
 */
enum MetaDataTypes {
  LIGANDOMICS("Ligandomics", "Detailed Explanation for Ligandomics"), METABOLOMICS("Metabolomics",
      "Detailed Explanation for Metabolomics"), TRANSCRIPTOMICS_GENOMICS("Transcriptomics/Genomics",
      "Detailed Explanation for Transcriptomics/Genomics"), PROTEOMICS("Proteomics",
      "Detailed Explanation for Proteomics");
  final String metaDataType;
  final String metaDataDescription;

  MetaDataTypes(String metaDataType, String metaDataDescription) {
    this.metaDataType = metaDataType;
    this.metaDataDescription = metaDataDescription;
  }
}
