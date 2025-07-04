package life.qbic.projectmanagement.application.measurement.validation;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP;

/**
 * <b>Validation Rule</b>
 * <p>
 * Validation rules represent a simple interface to execute on metadata, and provide a
 * {@link ValidationResult} with a report of the finding in the metadata.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface ValidationRule {

  /**
   * Executes the rule and returns {@link ValidationResult} object with a description of the
   * finding.
   *
   * @return {@link ValidationResult#successful()}, if the rule passed successfully without any
   * violations, else {@link ValidationResult#failures()}
   * @since 1.11.0
   */
  ValidationResult execute();

  final class Utils {

    static boolean isPooled(MeasurementRegistrationInformationPxP measurement) {
      return !measurement.samplePoolGroup().isBlank();
    }
  }
}

record HasDistinctLabels(
    Supplier<MeasurementRegistrationInformationPxP> metadataSupplier) implements ValidationRule {

  public HasDistinctLabels {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    var validationResult = ValidationResult.successful();
    if (Utils.isPooled(metadata)) {
      // only applies for pooled measurements
      var specificMetadata = metadata.specificMetadata().values().stream().toList();

      var specificMetadataTotal = specificMetadata.size();

      var distinctLabelsTotal = specificMetadata.stream()
          .map(MeasurementSpecificPxP::label)
          .distinct()
          .toList()
          .size();

      if (distinctLabelsTotal < specificMetadataTotal) {
        validationResult = validationResult.combine(ValidationResult.withFailures(
            List.of("Missing distinct labels for pool " + metadata.samplePoolGroup())));
      }
    }
    return validationResult;
  }
}

record MissingLabel(Supplier<MeasurementRegistrationInformationPxP> metadataSupplier) implements
    ValidationRule {

  public MissingLabel {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var validationResult = ValidationResult.successful();
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    var specificMetadata = metadata.specificMetadata().values().stream().toList();

    if (specificMetadata.stream().anyMatch(data -> data.label().isBlank())) {
      validationResult = validationResult.combine(ValidationResult.withFailures(
          List.of("Missing at least one label for pool " + metadata.samplePoolGroup())));
    }

    return validationResult;
  }
}

record MissingSpecificMetadataNGS(
    Supplier<MeasurementRegistrationInformationNGS> metadataSupplier) implements ValidationRule {

  public MissingSpecificMetadataNGS {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    var validationResult = ValidationResult.successful();
    for (var entry : metadata.specificMetadata().entrySet()) {
      if (hasMissingIndexI7(entry.getValue())) {
        validationResult = validationResult.combine(
            ValidationResult.withFailures(List.of("Missing indices i7 for " + entry.getKey())));
      }
    }
    return validationResult;
  }

  private static boolean hasMissingIndexI7(MeasurementSpecificNGS specificMetadata) {
    // Only i7 or i7 + i5 index combination is fine
    // Missing i7 is considered a violation of the rule
    return specificMetadata.indexI7().isBlank();
  }
}

record MissingIndices(Supplier<MeasurementRegistrationInformationNGS> metadataSupplier) implements
    ValidationRule {

  public MissingIndices {
    Objects.requireNonNull(metadataSupplier);
  }

  public ValidationResult execute() {
    var metadata = Objects.requireNonNull(metadataSupplier.get());

    if (isPooled(metadata)) {
      return new MissingSpecificMetadataNGS(() -> metadata).execute();
    }
    return ValidationResult.successful();
  }

  private static boolean isPooled(MeasurementRegistrationInformationNGS measurement) {
    return !measurement.samplePoolGroup().isBlank();
  }

}


