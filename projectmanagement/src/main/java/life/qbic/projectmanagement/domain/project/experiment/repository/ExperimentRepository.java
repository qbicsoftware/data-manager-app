package life.qbic.projectmanagement.domain.project.experiment.repository;

import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * Stores and retrieves Experiment aggregates
 */
public interface ExperimentRepository {

  void add(Experiment experiment);

  void update(Experiment experiment);

  Experiment find(ExperimentId id);

}
