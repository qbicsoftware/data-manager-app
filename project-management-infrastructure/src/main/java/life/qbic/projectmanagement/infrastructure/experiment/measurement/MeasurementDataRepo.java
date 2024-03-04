package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import life.qbic.projectmanagement.application.measurement.MeasurementService.NGSMeasurementWrapper;
import life.qbic.projectmanagement.application.measurement.MeasurementService.ProteomicsMeasurementWrapper;

/**
 * <b>Measurement Data Repo</b>
 *
 * <p>Interface for the actual data registration preparation</p>
 *
 * @since 1.0.0
 */
public interface MeasurementDataRepo {

  void addNGSMeasurement(NGSMeasurementWrapper ngsMeasurement);

  void addProtemicsMeasurement(ProteomicsMeasurementWrapper proteomicsMeasurement);

}
