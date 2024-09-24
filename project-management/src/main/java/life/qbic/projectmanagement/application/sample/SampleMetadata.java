package life.qbic.projectmanagement.application.sample;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;

/**
 * <b>Sample Metadata</b>
 *
 * <p>A simple sample metadata DTO to pass them within the application</p>
 *
 * @since 1.0.0
 */
public record SampleMetadata(String sampleId, String sampleCode,
                             AnalysisMethod analysisToBePerformed, String sampleName,
                             String biologicalReplicate, String experimentId,
                             long experimentalGroupId, OntologyTerm species, OntologyTerm specimen,
                             OntologyTerm analyte, String comment) {

  public static SampleMetadata createNew(AnalysisMethod analysisToBePerformed, String sampleName,
      String biologicalReplicate, String experimentId, long experimentalGroupId,
      OntologyTerm species, OntologyTerm specimen,
      OntologyTerm analyte, String comment) {
    return new SampleMetadata("", "", analysisToBePerformed, sampleName, biologicalReplicate,
        experimentId, experimentalGroupId,
        species, specimen, analyte, comment);
  }

  public static SampleMetadata createUpdate(String sampleId, String sampleCode,
      AnalysisMethod analysisToBePerformed,
      String sampleName,
      String biologicalReplicate, String experimentId, long experimentalGroupId,
      OntologyTerm species, OntologyTerm specimen,
      OntologyTerm analyte, String comment) {
    return new SampleMetadata(sampleId, sampleCode, analysisToBePerformed, sampleName,
        biologicalReplicate, experimentId, experimentalGroupId,
        species, specimen, analyte, comment);
  }

  public Optional<String> getSampleId() {
    return Optional.ofNullable(sampleId.isBlank() ? null : sampleId);
  }

  public Optional<String> getSampleCode() {
    return Optional.ofNullable(sampleCode.isBlank() ? null : sampleCode);
  }

  /**
   * Creates a deep copy of the current {@link SampleMetadata} object instance. The returned instance does not share
   * @return
   * @since
   */
  public SampleMetadata copy() {
    return new SampleMetadata(sampleId, sampleCode, analysisToBePerformed, sampleName,
        biologicalReplicate, experimentId, experimentalGroupId, species, specimen, analyte,
        comment);
  }

}
