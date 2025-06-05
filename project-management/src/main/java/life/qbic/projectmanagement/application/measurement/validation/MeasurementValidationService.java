package life.qbic.projectmanagement.application.measurement.validation;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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
  private final MeasurementProteomicsValidator measurementProteomicsValidator;

  @Autowired
  public MeasurementValidationService(MeasurementNGSValidator measurementNgsValidator,
      MeasurementProteomicsValidator pxpValidator,
      MeasurementProteomicsValidator measurementProteomicsValidator) {
    this.measurementNgsValidator = measurementNgsValidator;
    this.pxpValidator = pxpValidator;
    this.measurementProteomicsValidator = measurementProteomicsValidator;
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

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateNGS(MeasurementRegistrationInformationNGS registration,
      ProjectId projectId) {
    var metadata = new NGSMeasurementMetadata(
        null,
        registration.sampleCodes().stream().map(SampleCode::create).toList(),
        registration.organisationId(),
        registration.instrumentCURIE(),
        registration.facility(),
        registration.sequencingReadType(),
        registration.libraryKit(),
        registration.flowCell(),
        registration.sequencingRunProtocol(),
        registration.samplePoolGroup(),
        registration.indexI7(),
        registration.indexI5(),
        registration.comment()
    );
    return measurementNgsValidator.validate(metadata, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateNGS(MeasurementUpdateInformationNGS update, ProjectId projectId) {
    var metadata = new NGSMeasurementMetadata(
        update.measurementId(),
        update.sampleCodes().stream().map(SampleCode::create).toList(),
        update.organisationId(),
        update.instrumentCURIE(),
        update.facility(),
        update.sequencingReadType(),
        update.libraryKit(),
        update.flowCell(),
        update.sequencingRunProtocol(),
        update.samplePoolGroup(),
        update.indexI7(),
        update.indexI5(),
        update.comment()
    );
    return measurementNgsValidator.validateUpdate(metadata, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validatePxp(MeasurementRegistrationInformationPxP registration,
      ProjectId projectId) {
    var metadata = new ProteomicsMeasurementMetadata(
        null,
        registration.sampleCode(),
        registration.technicalReplicateName(),
        registration.organisationId(),
        registration.msDeviceCURIE(),
        registration.samplePoolGroup(),
        registration.facility(),
        registration.fractionName(),
        registration.digestionEnzyme(),
        registration.digestionMethod(),
        registration.enrichmentMethod(),
        registration.injectionVolume(),
        registration.lcColumn(),
        registration.lcmsMethod(),
        registration.labeling(),
        registration.comment()
    );
    return measurementProteomicsValidator.validate(metadata, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validatePxp(MeasurementUpdateInformationPxP update, ProjectId projectId) {
    var metadata = new ProteomicsMeasurementMetadata(
        update.measurementId(),
        update.sampleCode(),
        update.technicalReplicateName(),
        update.organisationId(),
        update.msDeviceCURIE(),
        update.samplePoolGroup(),
        update.facility(),
        update.fractionName(),
        update.digestionEnzyme(),
        update.digestionMethod(),
        update.enrichmentMethod(),
        update.injectionVolume(),
        update.lcColumn(),
        update.lcmsMethod(),
        update.labeling(),
        update.comment()
    );
    return measurementProteomicsValidator.validateUpdate(metadata, projectId);
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
