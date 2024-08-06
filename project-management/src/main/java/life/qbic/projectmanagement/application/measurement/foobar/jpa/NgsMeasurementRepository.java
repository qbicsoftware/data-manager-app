package life.qbic.projectmanagement.application.measurement.foobar.jpa;

import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import org.springframework.data.repository.ListCrudRepository;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface NgsMeasurementRepository extends
    ListCrudRepository<NGSMeasurement, MeasurementId> {

}
