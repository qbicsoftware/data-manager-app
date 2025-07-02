package life.qbic.projectmanagement.application.measurement.validation;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@FunctionalInterface
public interface ValidationRule {

  ValidationResult execute();

}

record MissingSpecificMetadataPxP(Supplier<MeasurementRegistrationInformationPxP> metadataSupplier) implements ValidationRule {

  public MissingSpecificMetadataPxP {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    if (isPooled(metadata)) {
      return new MissingSpecificMetadataPxP(() -> metadata).execute();
    }
    return ValidationResult.successful();
  }

  private static boolean isPooled(MeasurementRegistrationInformationPxP measurement) {
    return !measurement.samplePoolGroup().isBlank();
  }

}

record MissingLabel(Supplier<MeasurementRegistrationInformationPxP> metadataSupplier) implements ValidationRule {

  public MissingLabel {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var validationResult = ValidationResult.successful();
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    var specificMetadata = metadata.specificMetadata().entrySet().stream().map(Entry::getValue).toList();

    var specificMetadataTotal = specificMetadata.size();

    var distinctLabelsTotal = specificMetadata.stream()
        .map(MeasurementSpecificPxP::label)
        .distinct()
        .toList()
        .size();

    if (distinctLabelsTotal < specificMetadataTotal) {
      validationResult = validationResult.combine(validationResult.withFailures(List.of("Missing distinct labels for pool " + metadata.samplePoolGroup())));
      return validationResult;
    }
    return validationResult;
  }
}

record MissingSpecificMetadataNGS(Supplier<MeasurementRegistrationInformationNGS> metadataSupplier) implements ValidationRule {

  public MissingSpecificMetadataNGS {
    Objects.requireNonNull(metadataSupplier);
  }

  @Override
  public ValidationResult execute() {
    var metadata = Objects.requireNonNull(metadataSupplier.get());
    var validationResult = ValidationResult.successful();
    for (var entry : metadata.specificMetadata().entrySet()) {
      if (hasMissingIndices(entry.getValue())) {
        validationResult = validationResult.combine(validationResult.withFailures(List.of("Missing indices i7 or i5 for " + entry.getKey())));
      }
    }
    return validationResult;
  }

  private static boolean hasMissingIndices(MeasurementSpecificNGS specificMetadata) {
    return specificMetadata.indexI5().isBlank() || specificMetadata.indexI5().isBlank();
  }
}

record MissingIndices(Supplier<MeasurementRegistrationInformationNGS> metadataSupplier) implements ValidationRule {

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
