package life.qbic.datamanager.views.projects.project.measurements;

import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.ValidationResult;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Validation Executor</b>
 * <p>
 * An interface that enables injection of different configurations of
 * {@link MeasurementValidationService} calls,
 * based on the use case (create or edit).
 * <p>
 * To support more domains, just create more methods here.
 *
 * @since 1.0.0
 */
public interface MeasurementValidationExecutor<MeasurementMetadata> {

  CompletableFuture<ValidationResult> validateRegistration(MeasurementMetadata metadata, ProjectId projectId);

  CompletableFuture<ValidationResult> validateUpdate(MeasurementMetadata metadata, ProjectId projectId);

}
