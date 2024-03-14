package life.qbic.datamanager.views.projects.project.measurements;

import java.util.Collection;
import java.util.Optional;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.application.measurement.validation.ValidationService.Domain;

/**
 * <b>Validation Executor</b>
 * <p>
 * An interface that enables injection of different configurations of
 * {@link life.qbic.projectmanagement.application.measurement.validation.ValidationService} calls,
 * based on the use case (create or edit).
 * <p>
 * To support more domains, just create more methods here.
 *
 * @since 1.0.0
 */
public interface ValidationExecutor {

  ValidationResult validateNGS(NGSMeasurementMetadata metadata);

  ValidationResult validateProteomics(ProteomicsMeasurementMetadata metadata);

  Optional<Domain> inferDomainByProperties(Collection<String> properties);

}
