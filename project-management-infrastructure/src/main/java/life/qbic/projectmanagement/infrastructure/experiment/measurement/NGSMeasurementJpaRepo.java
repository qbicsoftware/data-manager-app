package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple NGS measurement JPA repository to query and filter concise
 * {@link NGSMeasurement} information
 */
public interface NGSMeasurementJpaRepo
    extends JpaRepository<NGSMeasurement, MeasurementId>,
    JpaSpecificationExecutor<NGSMeasurement> {

  @Override
  long count(Specification<NGSMeasurement> spec);
}
