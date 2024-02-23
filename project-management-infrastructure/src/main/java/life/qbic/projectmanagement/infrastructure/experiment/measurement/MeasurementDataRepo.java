package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.Collection;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MeasurementDataRepo {

  void addNGSMeasurements(Collection<NGSMeasurement> ngsMeasurements);

  void addNGSMeasurement(NGSMeasurement ngsMeasurement);

  void addProtemicsMeasurement(ProteomicsMeasurement proteomicsMeasurement);

}
