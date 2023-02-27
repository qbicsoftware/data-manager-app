package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */

@Repository
public class ExperimentJpaRepository implements ExperimentRepository {


  private QbicExperimentRepo qbicExperimentRepo;

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
  public Experiment find(ExperimentId id) {
    return qbicExperimentRepo.findExperimentDaoByExperimentId(id).stream().findFirst().orElse(null);
  }
}
