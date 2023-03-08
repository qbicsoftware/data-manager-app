package life.qbic.projectmanagement.experiment.persistence;

import java.util.List;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.data.repository.CrudRepository;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface QbicExperimentRepo extends CrudRepository<Experiment, ExperimentId> {

  List<Experiment> findExperimentByExperimentId(ExperimentId id);

}
