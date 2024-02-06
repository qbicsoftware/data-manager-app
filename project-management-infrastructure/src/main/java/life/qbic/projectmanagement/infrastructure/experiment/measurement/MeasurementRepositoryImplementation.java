package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService.ResponseCode;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementRepositoryImplementation implements MeasurementRepository {

  private static final Logger log = logger(MeasurementRepositoryImplementation.class);
  private final NGSMeasurementJpaRepo measurementJpaRepo;

  private final MeasurementDataRepo measurementDataRepo;

  public MeasurementRepositoryImplementation(NGSMeasurementJpaRepo measurementJpaRepo,
      MeasurementDataRepo measurementDataRepo) {
    this.measurementJpaRepo = measurementJpaRepo;
    this.measurementDataRepo = measurementDataRepo;
  }

  @Override
  public Result<NGSMeasurement, ResponseCode> save(NGSMeasurement measurement) {
    try {
      measurementJpaRepo.save(measurement);
    } catch (Exception e) {
      log.error("Saving ngs measurement failed", e);
      return Result.fromError(ResponseCode.FAILED);
    }

    try {
      measurementDataRepo.addNGSMeasurement(measurement);
    } catch (Exception e) {
      log.error("Saving ngs measurement in data repo failed for measurement "
          + measurement.measurementCode().value(), e);
      measurementJpaRepo.delete(measurement); // Rollback JPA save
      return Result.fromError(ResponseCode.FAILED);
    }

    return Result.fromValue(measurement);
  }

  @Override
  public Result<Collection<NGSMeasurement>, ResponseCode> saveAll(
      Collection<NGSMeasurement> ngsMeasurements) {
    return null;
  }
}
