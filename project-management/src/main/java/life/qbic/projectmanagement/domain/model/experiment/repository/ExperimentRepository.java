package life.qbic.projectmanagement.domain.model.experiment.repository;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * Finds creates and updates Experiment Aggregates.
 */
public interface ExperimentRepository {

  void add(Experiment experiment);

  void update(Experiment experiment);

  Optional<Experiment> find(ExperimentId id);

  void delete(ExperimentId experimentId);

  Optional<String> findProjectId(ExperimentId experimentId);
}
