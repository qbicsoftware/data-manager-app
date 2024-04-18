package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService.ResponseCode;

/**
 * <b>Measurement Repository</b>
 * <p>
 * Persistent repository for different
 * {@link life.qbic.projectmanagement.application.measurement.MeasurementMetadata} implementations
 *
 * @since <version tag>
 */
public interface MeasurementRepository {

  Result<NGSMeasurement, ResponseCode> save(NGSMeasurement measurement, List<SampleCode> sampleCodes);

  Result<ProteomicsMeasurement, ResponseCode> save(ProteomicsMeasurement measurement, List<SampleCode> sampleCodes);

  Optional<ProteomicsMeasurement> find(String measurementCode);

  void update(ProteomicsMeasurement measurement);

  void updateAll(Collection<ProteomicsMeasurement> measurement);

  void saveAll(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping);
}
