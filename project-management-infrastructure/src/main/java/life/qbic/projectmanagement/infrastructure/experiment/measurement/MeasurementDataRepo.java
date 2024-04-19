package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import java.util.Map;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
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

  void deleteProteomicsMeasurements(List<ProteomicsMeasurement> measurements);
  void deleteNGSMeasurements(List<NGSMeasurement> measurements);

  boolean hasDataAttached(Set<? extends MeasurementMetadata> measurements);
  void saveAll(Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping);
}
