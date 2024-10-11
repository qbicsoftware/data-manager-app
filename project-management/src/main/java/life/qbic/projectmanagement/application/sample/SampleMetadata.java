package life.qbic.projectmanagement.application.sample;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <b>Sample Metadata</b>
 *
 * <p>A simple sample metadata DTO to pass them within the application</p>
 *
 * @since 1.0.0
 */
public record SampleMetadata(
    SampleId sampleId,
    String sampleCode,
    String sampleName,
    AnalysisMethod analysisToBePerformed,
    String biologicalReplicate,
    long experimentalGroupId,
    OntologyTerm species,
    OntologyTerm specimen,
    OntologyTerm analyte,
    String comment,
    String experimentId
) {

  public static SampleMetadata createNew(String sampleName,
      AnalysisMethod analysisToBePerformed,
      String biologicalReplicate,
      long experimentalGroupId,
      OntologyTerm species,
      OntologyTerm specimen,
      OntologyTerm analyte,
      String comment,
      String experimentId) {
    return new SampleMetadata(null, "", sampleName, analysisToBePerformed, biologicalReplicate,
        experimentalGroupId, species, specimen, analyte, comment, experimentId);
  }

  public static SampleMetadata createUpdate(SampleId sampleId,
      String sampleCode,
      String sampleName,
      AnalysisMethod analysisToBePerformed,
      String biologicalReplicate,
      long experimentalGroupId,
      OntologyTerm species,
      OntologyTerm specimen,
      OntologyTerm analyte,
      String comment,
      String experimentId) {
    return new SampleMetadata(sampleId, sampleCode, sampleName, analysisToBePerformed,
        biologicalReplicate, experimentalGroupId, species, specimen, analyte, comment,
        experimentId);
  }

  public Optional<SampleId> getSampleId() {
    return Optional.ofNullable(sampleId);
  }

  public Optional<String> getSampleCode() {
    return Optional.ofNullable(sampleCode.isBlank() ? null : sampleCode);
  }


}
