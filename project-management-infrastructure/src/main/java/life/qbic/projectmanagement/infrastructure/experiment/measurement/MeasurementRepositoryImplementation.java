package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.MeasurementService.NGSMeasurementWrapper;
import life.qbic.projectmanagement.application.measurement.MeasurementService.ProteomicsMeasurementWrapper;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService.ResponseCode;
import org.springframework.stereotype.Repository;

/**
 * <b>Measurement Repository Implementation</b>
 *
 * <p>Implementation of the {@link MeasurementRepository} interface</p>
 *
 * @since 1.0.0
 */
@Repository
public class MeasurementRepositoryImplementation implements MeasurementRepository {

  private static final Logger log = logger(MeasurementRepositoryImplementation.class);
  private final NGSMeasurementJpaRepo measurementJpaRepo;
  private final ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo;
  private final MeasurementDataRepo measurementDataRepo;

  public MeasurementRepositoryImplementation(NGSMeasurementJpaRepo measurementJpaRepo,
      ProteomicsMeasurementJpaRepo pxpMeasurenemtJpaRepo,
      MeasurementDataRepo measurementDataRepo) {
    this.measurementJpaRepo = measurementJpaRepo;
    this.pxpMeasurementJpaRepo = pxpMeasurenemtJpaRepo;
    this.measurementDataRepo = measurementDataRepo;
  }

  @Override
  public Result<NGSMeasurement, ResponseCode> save(NGSMeasurementWrapper measurementWithCodes) {
    NGSMeasurement measurement = measurementWithCodes.measurementMetadata();
    try {
      measurementJpaRepo.save(measurementWithCodes.measurementMetadata());
    } catch (Exception e) {
      log.error("Saving ngs measurement failed", e);
      return Result.fromError(ResponseCode.FAILED);
    }

    try {
      measurementDataRepo.addNGSMeasurement(measurementWithCodes);
    } catch (Exception e) {
      log.error("Saving ngs measurement in data repo failed for measurement "
          + measurement.measurementCode().value(), e);
      measurementJpaRepo.delete(measurement); // Rollback JPA save
      return Result.fromError(ResponseCode.FAILED);
    }

    return Result.fromValue(measurement);
  }

  @Override
  public Result<ProteomicsMeasurement, ResponseCode> save(
      ProteomicsMeasurementWrapper measurementWithCodes) {
    ProteomicsMeasurement measurement = measurementWithCodes.measurementMetadata();
    try {
      pxpMeasurementJpaRepo.save(measurement);
    } catch (Exception e) {
      log.error("Saving proteomics measurement failed", e);
      return Result.fromError(ResponseCode.FAILED);
    }

    try {
      measurementDataRepo.addProtemicsMeasurement(measurementWithCodes);
    } catch (Exception e) {
      log.error("Saving ngs measurement in data repo failed for measurement "
          + measurement.measurementCode().value(), e);
      pxpMeasurementJpaRepo.delete(measurement); // Rollback JPA save
      return Result.fromError(ResponseCode.FAILED);
    }

    return Result.fromValue(measurement);
  }

}
