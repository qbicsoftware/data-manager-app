package life.qbic.projectmanagement.application.sample;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SampleRegistrationServiceV2 {

  private final BatchRegistrationService batchRegistrationService;
  private final SampleRepository sampleRepository;

  public SampleRegistrationServiceV2(BatchRegistrationService batchRegistrationService, SampleRepository sampleRepository) {
    this.batchRegistrationService = Objects.requireNonNull(batchRegistrationService);
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public CompletableFuture<Void> registerNewSamples(Collection<SampleMetadata> sampleMetadata,
      ExperimentId experimentId, ProjectId projectId, String batchLabel, boolean batchIsPilot) {

    throw new UnsupportedOperationException("Not implemented yet");
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public CompletableFuture<Void> updateSamples(Collection<SampleMetadata> sampleMetadata,
      ExperimentId experimentId, ProjectId projectId) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
