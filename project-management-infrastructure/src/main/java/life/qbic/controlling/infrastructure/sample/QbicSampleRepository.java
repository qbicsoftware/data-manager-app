package life.qbic.controlling.infrastructure.sample;

import java.util.Collection;
import life.qbic.controlling.domain.model.experiment.ExperimentId;
import life.qbic.controlling.domain.model.sample.Sample;
import life.qbic.controlling.domain.model.sample.SampleId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QbicSampleRepository extends JpaRepository<Sample, SampleId> {

    Collection<Sample> findAllByExperimentId(ExperimentId experimentId);
}
