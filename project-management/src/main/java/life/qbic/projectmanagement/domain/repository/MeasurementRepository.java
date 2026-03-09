package life.qbic.projectmanagement.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementCode);

  Optional<NGSMeasurement> findNGSMeasurement(String measurementCode);

  Optional<NGSMeasurement> findNGSMeasurementById(String measurementId);

  void updateProteomics(ProteomicsMeasurement measurement);

  Optional<ProteomicsMeasurement> findProteomicsMeasurementById(String measurementCode);

  void deleteAllProteomics(Set<String> measurementIds);

  void deleteAllNgs(Set<String> measurementIds);

  void updateNGS(NGSMeasurement measurement);

  void updateAllProteomics(Collection<ProteomicsMeasurement> measurement);

  void updateAllNGS(Collection<NGSMeasurement> measurement);

  void saveAllProteomics(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping);

  void saveAllNGS(Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping);

  boolean existsMeasurement(String measurementCode);
}
