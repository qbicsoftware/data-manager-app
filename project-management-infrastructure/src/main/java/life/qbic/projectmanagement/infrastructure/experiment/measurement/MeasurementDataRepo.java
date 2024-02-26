package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.application.measurement.MeasurementService.NGSMeasurementWrapper;
import life.qbic.projectmanagement.application.measurement.MeasurementService.ProteomicsMeasurementWrapper;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MeasurementDataRepo {

  void addNGSMeasurement(NGSMeasurementWrapper ngsMeasurement);

  void addProtemicsMeasurement(ProteomicsMeasurementWrapper proteomicsMeasurement);

}
