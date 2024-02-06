package life.qbic.projectmanagement.domain.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementDomainService {

  private static final Logger log = logger(MeasurementDomainService.class);

  private final MeasurementRepository measurementRepository;

  public MeasurementDomainService(MeasurementRepository measurementRepository) {
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }

  public Result<NGSMeasurement, ResponseCode> addNGS(NGSMeasurement ngsMeasurement) {
    try {
      measurementRepository.save(ngsMeasurement);
      return Result.fromValue(ngsMeasurement);
    } catch (Exception e) {
      log.error(
          "Saving the NGS measurement failed for id: " + ngsMeasurement.measurementCode().value());
    }
    return Result.fromValue(ngsMeasurement);
  }

  public enum ResponseCode {
    SUCCESSFUL, FAILED, MEASUREMENT_EXISTS
  }

}
