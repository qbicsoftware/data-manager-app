package life.qbic.controlling.infrastructure.experiment;

import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A experiment repository interface implemented by spring.
 */
public interface QbicExperimentRepo extends JpaRepository<Experiment, ExperimentId> {

  List<Experiment> findExperimentByExperimentId(ExperimentId id);

}
