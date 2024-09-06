package life.qbic.projectmanagement.application.sample;

import java.util.Objects;
import life.qbic.projectmanagement.application.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class SampleValidationService {

  private final SampleValidation sampleValidation;

  @Autowired
  public SampleValidationService(SampleValidation sampleValidation) {
    this.sampleValidation = Objects.requireNonNull(sampleValidation);
  }

  public ValidationResult validateNewSample(SampleMetadata sampleMetadata, String experimentId,
      String projectId) {
    return sampleValidation.validateNewSample(sampleMetadata, experimentId, projectId);
  }


}
