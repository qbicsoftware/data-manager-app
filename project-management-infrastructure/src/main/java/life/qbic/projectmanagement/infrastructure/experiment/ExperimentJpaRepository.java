package life.qbic.projectmanagement.infrastructure.experiment;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * A experiment repository implementing the experiment repository interface dictated by the domain.
 */

@Repository
public class ExperimentJpaRepository implements ExperimentRepository {


  private final QbicExperimentRepo qbicExperimentRepo;

  @Autowired
  public ExperimentJpaRepository(QbicExperimentRepo qbicExperimentRepo) {
    this.qbicExperimentRepo = qbicExperimentRepo;
  }

  @Override
  public void add(Experiment experiment) {
    qbicExperimentRepo.save(experiment);
  }

  @Override
  public void update(Experiment experiment) {
    qbicExperimentRepo.save(experiment);
  }

  @Override
  public Optional<Experiment> find(ExperimentId id) {
    return qbicExperimentRepo.findExperimentByExperimentId(id).stream().findFirst();
  }

  @Override
  public void delete(ExperimentId experimentId) {
    qbicExperimentRepo.deleteById(experimentId);
  }

  @Override
  public Optional<String> findProjectId(ExperimentId experimentId) {
    return qbicExperimentRepo.findProjectIdForExperiment(experimentId.value());
  }
}
