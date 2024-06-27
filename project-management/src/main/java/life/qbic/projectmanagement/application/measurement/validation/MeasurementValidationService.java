package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateNGS(NGSMeasurementMetadata ngsMeasurementMetadata,
      ProjectId projectId) {
    return measurementNgsValidator.validate(ngsMeasurementMetadata, projectId);
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
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateProteomics(ProteomicsMeasurementMetadata pxMeasurementMetadata,
      ProjectId projectId) {
    return pxpValidator.validate(pxMeasurementMetadata, projectId);
  }

  /**
   * Validates proteomic measurement metadata in the case of an update of a registered measurement.
   *
   * @param pxMeasurementMetadata the measurement to validate
   * @return a detailed {@link ValidationResult} with information about the validation
   * @since 1.0.0
   */
  @Async
  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public CompletableFuture<ValidationResult> validateProteomicsUpdate(
      ProteomicsMeasurementMetadata pxMeasurementMetadata, ProjectId projectId) {
    var result = pxpValidator.validateUpdate(pxMeasurementMetadata, projectId);
    return CompletableFuture.completedFuture(result);
  }

  /**
   * Validates ngs measurement metadata in the case of an update of a registered measurement.
   *
   * @param ngsMeasurementMetadata the measurement to validate
   * @return a detailed {@link ValidationResult} with information about the validation
   * @since 1.0.0
   */
  @Async
  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public CompletableFuture<ValidationResult> validateNGSUpdate(
      NGSMeasurementMetadata ngsMeasurementMetadata,
      ProjectId projectId) {
    var result = measurementNgsValidator.validateUpdate(ngsMeasurementMetadata, projectId);
    return CompletableFuture.completedFuture(result);
  }

  public Optional<Domain> inferDomainByPropertyTypes(Collection<String> propertyTypes) {
    return Optional.ofNullable(determineDomain(propertyTypes));
  }

  public enum Domain {
    NGS, PROTEOMICS
  }


}
