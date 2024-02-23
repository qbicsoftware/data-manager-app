package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import org.springframework.data.repository.CrudRepository;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface ProteomicsMeasurementJpaRepo extends
    CrudRepository<ProteomicsMeasurement, MeasurementId> {

}
