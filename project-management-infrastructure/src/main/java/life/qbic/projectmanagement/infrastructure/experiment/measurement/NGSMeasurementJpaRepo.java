package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple NGS measurement JPA repository to search and filter concise
 * {@link NGSMeasurement} information
 */
public interface NGSMeasurementJpaRepo
    extends JpaRepository<NGSMeasurement, MeasurementId>,
    JpaSpecificationExecutor<NGSMeasurement> {

  @Override
  long count(Specification<NGSMeasurement> spec);

  Optional<NGSMeasurement> findNGSMeasurementByMeasurementCode(
      MeasurementCode measurementCode);

  Optional<NGSMeasurement> findNGSMeasurementByMeasurementId(
      MeasurementId measurementId);
}
