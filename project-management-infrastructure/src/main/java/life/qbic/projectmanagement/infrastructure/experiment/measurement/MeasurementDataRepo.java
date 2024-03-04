package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.List;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;

/**
 * <b>Measurement Data Repo</b>
 *
 * <p>Interface for the actual data registration preparation</p>
 *
 * @since 1.0.0
 */
public interface MeasurementDataRepo {

  void addNGSMeasurement(NGSMeasurement ngsMeasurement, List<SampleCode> sampleCodes);

  void addProtemicsMeasurement(ProteomicsMeasurement proteomicsMeasurement, List<SampleCode> sampleCodes);

}
