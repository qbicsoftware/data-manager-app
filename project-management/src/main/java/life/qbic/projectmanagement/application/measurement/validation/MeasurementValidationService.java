package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * <b>Validation Service</b>
 *
 * <p>Validation Service for measurement metadata validation prior to registration</p>
 * <p>
 * This service can be used to validate provided measurement metadata before register them. Of
 * course registration will also contain validation, consider this service as a try-run registration
 * service to let the user fail early with detailed provided {@link ValidationResult}.
 *
 * @since 1.0.0
 */
@Service
public class MeasurementValidationService {

  private final MeasurementNGSValidator measurementNgsValidator;

  private final MeasurementProteomicsValidator pxpValidator;

  @Autowired
  public MeasurementValidationService(MeasurementNGSValidator measurementNgsValidator,
      MeasurementProteomicsValidator pxpValidator) {
    this.measurementNgsValidator = measurementNgsValidator;
    this.pxpValidator = pxpValidator;
  }

  private static Domain determineDomain(Collection<String> propertyTypes) {
    if (MeasurementNGSValidator.isNGS(propertyTypes)) {
      return Domain.NGS;
    }
    if (MeasurementProteomicsValidator.isProteomics(propertyTypes)) {
      return Domain.PROTEOMICS;
    }
    return null;
  }

  public ValidationResult validateNGS(NGSMeasurementMetadata ngsMeasurementMetadata) {
    return measurementNgsValidator.validate(ngsMeasurementMetadata);
  }

  /**
   * This method validates a proteomic measurement metadata object in the case of a new measurement
   * that is going to be registered.
   * <p>
   * It will validate all properties exhaustive, but also is not aware of any present measurement
   * id, which also will not get validated.
   * <p>
   * If you want to validate a measurement update, please use
   *
   * @param pxMeasurementMetadata the measurement to validate
   * @return a detailed {@link ValidationResult} with information about the validation
   * @since 1.0.0
   */
  public ValidationResult validateProteomics(ProteomicsMeasurementMetadata pxMeasurementMetadata) {
    return pxpValidator.validate(pxMeasurementMetadata);
  }

  /**
   * Validates proteomic measurement metadata in the case of an update of a registered measurement.
   *
   * @param pxMeasurementMetadata the measurement to validate
   * @return a detailed {@link ValidationResult} with information about the validation
   * @since 1.0.0
   */
  @Async
  public CompletableFuture<ValidationResult> validateProteomicsUpdate(
      ProteomicsMeasurementMetadata pxMeasurementMetadata) {
    return CompletableFuture.supplyAsync(() -> pxpValidator.validateUpdate(pxMeasurementMetadata));
  }

  public ValidationResult validateNGSUpdate(NGSMeasurementMetadata ngsMeasurementMetadata) {
    return measurementNgsValidator.validate(ngsMeasurementMetadata);
  }

  public Optional<Domain> inferDomainByPropertyTypes(Collection<String> propertyTypes) {
    return Optional.ofNullable(determineDomain(propertyTypes));
  }

  public enum Domain {
    NGS, PROTEOMICS
  }


}
