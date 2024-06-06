package life.qbic.projectmanagement.domain.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class MeasurementDomainService {

  private static final Logger log = logger(MeasurementDomainService.class);

  private final MeasurementRepository measurementRepository;

  @Autowired
  public MeasurementDomainService(MeasurementRepository measurementRepository) {
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }

  public Result<NGSMeasurement, ResponseCode> addNGS(NGSMeasurement measurement,
      List<SampleCode> sampleCodes) {
    try {
      measurementRepository.save(measurement, sampleCodes);
      return Result.fromValue(measurement);
    } catch (RuntimeException e) {
      log.error(
          "Saving the NGS measurement failed for id: " + measurement.measurementCode().value(), e);
    }
    return Result.fromError(ResponseCode.FAILED);
  }

  public List<MeasurementId> addNGSAll(
      Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping) {
    measurementRepository.saveAllNGS(ngsMeasurementsMapping);
    return ngsMeasurementsMapping.keySet().stream().map(NGSMeasurement::measurementId)
        .toList();
  }

  public Result<ProteomicsMeasurement, ResponseCode> addProteomics(
      ProteomicsMeasurement measurement, List<SampleCode> sampleCodes) {
    try {
      measurementRepository.save(measurement, sampleCodes);
      return Result.fromValue(measurement);
    } catch (RuntimeException e) {
      log.error(
          "Saving the Proteomics measurement failed for id: " + measurement.measurementCode()
              .value(), e);
    }
    return Result.fromError(ResponseCode.FAILED);
  }

  public List<MeasurementId> addProteomicsAll(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping) {
    measurementRepository.saveAllProteomics(proteomicsMeasurementsMapping);
    return proteomicsMeasurementsMapping.keySet().stream().map(ProteomicsMeasurement::measurementId)
        .toList();
  }

  public Result<NGSMeasurement, ResponseCode> updateNGS(NGSMeasurement measurement) {
    try {
      measurementRepository.updateNGS(measurement);
      return Result.fromValue(measurement);
    } catch (RuntimeException e) {
      log.error("Measurement update: Failed for measurement with id " + measurement.measurementId()
          .value(), e);
    }
    return Result.fromError(ResponseCode.FAILED);
  }

  public void deleteNGS(Set<NGSMeasurement> measurements) {
    measurementRepository.deleteAllNGS(measurements);
  }

  public void deletePxP(Set<ProteomicsMeasurement> measurements) {
    measurementRepository.deleteAllProteomics(measurements);
  }

  public List<MeasurementId> updateProteomicsAll(
      List<ProteomicsMeasurement> proteomicsMeasurements) {
    measurementRepository.updateAllProteomics(proteomicsMeasurements);
    return proteomicsMeasurements.stream().map(ProteomicsMeasurement::measurementId).toList();
  }

  public List<MeasurementId> updateNGSAll(List<NGSMeasurement> ngsMeasurements) {
    measurementRepository.updateAllNGS(ngsMeasurements);
    return ngsMeasurements.stream().map(NGSMeasurement::measurementId).toList();
  }

  public enum ResponseCode {
    SUCCESSFUL, FAILED, MEASUREMENT_EXISTS
  }

}
