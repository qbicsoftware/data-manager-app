package life.qbic.projectmanagement.domain.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
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

  public enum ResponseCode {
    SUCCESSFUL, FAILED, MEASUREMENT_EXISTS
  }

  public Result<ProteomicsMeasurement, ResponseCode> update(ProteomicsMeasurement measurement) {
    try {
      measurementRepository.update(measurement);
      return Result.fromValue(measurement);
    } catch (RuntimeException e) {
      log.error("Measurement update: Failed for measurement with id " + measurement.measurementId()
          .value(), e);
    }
    return Result.fromError(ResponseCode.FAILED);
  }

  public void delete(Set<? extends MeasurementMetadata> measurements) {
      measurementRepository.deleteAll(measurements);
  }

}
