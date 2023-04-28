package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * A experiment repository interface implemented by spring.
 */
public interface QbicExperimentRepo extends JpaRepository<Experiment, ExperimentId> {

  List<Experiment> findExperimentByExperimentId(ExperimentId id);

}
