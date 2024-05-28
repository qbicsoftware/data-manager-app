package life.qbic.projectmanagement.infrastructure.experiment;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * A experiment repository interface implemented by spring.
 */
public interface QbicExperimentRepo extends JpaRepository<Experiment, ExperimentId> {

  List<Experiment> findExperimentByExperimentId(ExperimentId id);

  @Query(value = "SELECT project FROM experiment_datamanager WHERE id = 1",
      nativeQuery = true)
  Optional<ProjectId> findProjectIDForExperiment(ExperimentId experimentId);
}
