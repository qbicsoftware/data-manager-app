package life.qbic.projectmanagement.application.api;

import java.util.Map;
import java.util.Optional;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
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
    Map<ConfoundingVariableInformation, String> confoundingVariables,
    String experimentId
) implements ValidationRequestBody {

  public static SampleMetadata createNew(String sampleName,
      AnalysisMethod analysisToBePerformed,
      String biologicalReplicate,
      long experimentalGroupId,
      OntologyTerm species,
      OntologyTerm specimen,
      OntologyTerm analyte,
      String comment,
      Map<ConfoundingVariableInformation, String> confoundingVariables,
      String experimentId) {
    return new SampleMetadata(null, "", sampleName, analysisToBePerformed, biologicalReplicate,
        experimentalGroupId, species, specimen, analyte, comment, confoundingVariables,
        experimentId);
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
      Map<ConfoundingVariableInformation, String> confoundingVariables,
      String experimentId) {
    return new SampleMetadata(sampleId, sampleCode, sampleName, analysisToBePerformed,
        biologicalReplicate, experimentalGroupId, species, specimen, analyte, comment,
        confoundingVariables,
        experimentId);
  }

  public static SampleMetadata addSampleId(SampleId id, SampleMetadata sampleMetadata) {
    return new SampleMetadata(
        id, sampleMetadata.sampleCode, sampleMetadata.sampleName,
        sampleMetadata.analysisToBePerformed, sampleMetadata.biologicalReplicate,
        sampleMetadata.experimentalGroupId, sampleMetadata.species, sampleMetadata.specimen,
        sampleMetadata.analyte, sampleMetadata.comment,
        sampleMetadata.confoundingVariables,
        sampleMetadata.experimentId
    );
  }

  public Optional<SampleId> getSampleId() {
    return Optional.ofNullable(sampleId);
  }

  public Optional<String> getSampleCode() {
    return Optional.ofNullable(sampleCode.isBlank() ? null : sampleCode);
  }


}
