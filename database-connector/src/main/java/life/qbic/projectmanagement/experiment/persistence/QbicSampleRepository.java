package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QbicSampleRepository extends JpaRepository<Sample, SampleId> {

}
