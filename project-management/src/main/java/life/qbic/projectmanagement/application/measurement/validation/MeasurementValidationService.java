package life.qbic.projectmanagement.application.measurement.validation;

import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final MeasurementProteomicsValidator measurementProteomicsValidator;

  @Autowired
  public MeasurementValidationService(MeasurementNGSValidator measurementNgsValidator,
      MeasurementProteomicsValidator measurementProteomicsValidator) {
    this.measurementNgsValidator = measurementNgsValidator;
    this.measurementProteomicsValidator = measurementProteomicsValidator;
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateNGS(MeasurementRegistrationInformationNGS registration,
      String experimentId,
      ProjectId projectId) {
    return measurementNgsValidator.validateRegistration(registration, experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validateNGS(MeasurementUpdateInformationNGS update, String experimentId,
      ProjectId projectId) {
    return measurementNgsValidator.validateUpdate(update, experimentId, projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validatePxp(MeasurementRegistrationInformationPxP registration,
      String experimentId, ProjectId projectId) {
    return measurementProteomicsValidator.validateRegistration(registration, experimentId,
        projectId);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public ValidationResult validatePxp(MeasurementUpdateInformationPxP update, String experimentId,
      ProjectId projectId) {
    return measurementProteomicsValidator.validateUpdate(update, experimentId, projectId);
  }

  public enum Domain {
    NGS, PROTEOMICS
  }


}
