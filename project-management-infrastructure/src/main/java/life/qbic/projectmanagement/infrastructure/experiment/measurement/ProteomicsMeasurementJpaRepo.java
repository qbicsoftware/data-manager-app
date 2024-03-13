package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple proteomics measurement JPA repository to query and filter concise
 * {@link ProteomicsMeasurement} information
 */
public interface ProteomicsMeasurementJpaRepo extends
    JpaRepository<ProteomicsMeasurement, MeasurementId>,
    JpaSpecificationExecutor<ProteomicsMeasurement> {

  @Override
  long count(Specification<ProteomicsMeasurement> spec);

}
