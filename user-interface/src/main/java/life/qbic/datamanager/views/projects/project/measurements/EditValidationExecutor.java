package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class EditValidationExecutor implements ValidationExecutor {

  private final ValidationService validationService;

  @Autowired
  public EditValidationExecutor(ValidationService validationService) {
    this.validationService = Objects.requireNonNull(validationService);
  }

  @Override
  public ValidationResult validateNGS(NGSMeasurementMetadata metadata) {
    // TODO call the edit validation method once we have them
    return validationService.validateNGS(metadata);
  }

  @Override
  public ValidationResult validateProteomics(ProteomicsMeasurementMetadata metadata) {
    // TODO call the edit validation method once we have them
    return validationService.validateProteomics(metadata);
  }

  @Override
  public Optional<Domain> inferDomainByProperties(Collection<String> properties) {
    // TODO call the edit infer domain method once we have it
    return validationService.inferDomainByPropertyTypes(properties);
  }


}
