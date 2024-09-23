package life.qbic.projectmanagement.application.sample;

import java.util.Optional;

/**
 * <b>Sample Metadata</b>
 *
 * <p>A simple sample metadata DTO to pass them within the application</p>
 *
 * @since 1.0.0
 */
public record SampleMetadata(String sampleId, String sampleCode, String analysisToBePerformed, String sampleName,
                             String biologicalReplicate, String condition, String species, String specimen,
                             String analyte, String comment) {

  public static SampleMetadata createNew(String analysisToBePerformed, String sampleName,
      String biologicalReplicate, String condition, String species, String specimen,
      String analyte, String comment) {
    return new SampleMetadata("", "", analysisToBePerformed, sampleName, biologicalReplicate, condition,
        species, specimen, analyte, comment);
  }

  public static SampleMetadata createUpdate(String sampleId, String sampleCode, String analysisToBePerformed,
      String sampleName,
      String biologicalReplicate, String condition, String species, String specimen,
      String analyte, String comment) {
    return new SampleMetadata(sampleId, sampleCode, analysisToBePerformed, sampleName, biologicalReplicate,
        condition, species, specimen, analyte, comment);
  }

  public Optional<String> getSampleId() {
    return Optional.ofNullable(sampleId.isBlank() ? null : sampleId);
  }

  public Optional<String> getSampleCode() {
    return Optional.ofNullable(sampleCode.isBlank() ? null : sampleCode);
  }

}
