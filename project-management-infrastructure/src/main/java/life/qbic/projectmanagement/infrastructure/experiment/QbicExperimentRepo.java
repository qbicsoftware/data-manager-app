package life.qbic.projectmanagement.infrastructure.experiment;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A experiment repository interface implemented by spring.
 */
public interface QbicExperimentRepo extends JpaRepository<Experiment, ExperimentId> {

  List<Experiment> findExperimentByExperimentId(ExperimentId id);
//TODO SVEN WHERE IS MY PROJECT

  @Override
  Optional<Experiment> find
}
