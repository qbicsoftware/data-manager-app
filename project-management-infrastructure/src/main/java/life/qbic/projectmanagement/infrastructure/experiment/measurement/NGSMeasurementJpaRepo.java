package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface NGSMeasurementJpaRepo extends JpaRepository<NGSMeasurement, MeasurementId> {

}
