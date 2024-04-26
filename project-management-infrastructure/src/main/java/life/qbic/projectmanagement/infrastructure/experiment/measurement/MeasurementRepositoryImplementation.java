package life.qbic.projectmanagement.infrastructure.experiment.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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
  private final NGSMeasurementJpaRepo ngsMeasurementJpaRepo;
  private final ProteomicsMeasurementJpaRepo pxpMeasurementJpaRepo;
  private final MeasurementDataRepo measurementDataRepo;

  public MeasurementRepositoryImplementation(NGSMeasurementJpaRepo ngsMeasurementJpaRepo,
      ProteomicsMeasurementJpaRepo pxpMeasurenemtJpaRepo,
      MeasurementDataRepo measurementDataRepo) {
    this.ngsMeasurementJpaRepo = ngsMeasurementJpaRepo;
    this.pxpMeasurementJpaRepo = pxpMeasurenemtJpaRepo;
    this.measurementDataRepo = measurementDataRepo;
  }

  @Override
  public Result<NGSMeasurement, ResponseCode> save(NGSMeasurement measurement, List<SampleCode> sampleCodes) {
    try {
      ngsMeasurementJpaRepo.save(measurement);
    } catch (RuntimeException e) {
      log.error("Saving ngs measurement failed", e);
      return Result.fromError(ResponseCode.FAILED);
    }
    try {
      measurementDataRepo.addNGSMeasurement(measurement, sampleCodes);
    } catch (RuntimeException e) {
      log.error("Saving ngs measurement in data repo failed for measurement "
          + measurement.measurementCode().value(), e);
      ngsMeasurementJpaRepo.delete(measurement); // Rollback JPA save
      return Result.fromError(ResponseCode.FAILED);
    }

    return Result.fromValue(measurement);
  }

  @Override
  public Result<ProteomicsMeasurement, ResponseCode> save(
      ProteomicsMeasurement measurement, List<SampleCode> sampleCodes) {
    try {
      pxpMeasurementJpaRepo.save(measurement);
    } catch (RuntimeException e) {
      log.error("Saving proteomics measurement failed", e);
      return Result.fromError(ResponseCode.FAILED);
    }
    try {
      measurementDataRepo.addProtemicsMeasurement(measurement, sampleCodes);
    } catch (RuntimeException e) {
      log.error("Saving proteomics measurement in data repo failed for measurement "
          + measurement.measurementCode().value(), e);
      pxpMeasurementJpaRepo.delete(measurement); // Rollback JPA save
      return Result.fromError(ResponseCode.FAILED);
    }

    return Result.fromValue(measurement);
  }

  @Override
  public Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementCode) {
    try {
      var code = MeasurementCode.parse(measurementCode);
      return pxpMeasurementJpaRepo.findProteomicsMeasurementByMeasurementCode(code);
    } catch (IllegalArgumentException e) {
      log.error("Illegal measurement code: " + measurementCode, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<NGSMeasurement> findNGSMeasurement(String measurementCode) {
    try {
      var code = MeasurementCode.parse(measurementCode);
      return ngsMeasurementJpaRepo.findNGSMeasurementByMeasurementCode(code);
    } catch (IllegalArgumentException e) {
      log.error("Illegal measurement code: " + measurementCode, e);
      return Optional.empty();
    }
  }

  @Override
  public void updateProteomics(ProteomicsMeasurement measurement) {
    pxpMeasurementJpaRepo.save(measurement);
  }

  @Override
  public void updateNGS(NGSMeasurement measurement) {
    ngsMeasurementJpaRepo.save(measurement);
  }

  @Override
  public void updateAllProteomics(Collection<ProteomicsMeasurement> measurements) {
    pxpMeasurementJpaRepo.saveAll(measurements);
  }

  @Override
  public void updateAllNGS(Collection<NGSMeasurement> measurements) {
    ngsMeasurementJpaRepo.saveAll(measurements);
  }

  @Override
  public void saveAllProteomics(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping) {
    try {
      pxpMeasurementJpaRepo.saveAll(proteomicsMeasurementsMapping.keySet());
    } catch (RuntimeException e) {
      log.error("Saving proteomics measurement failed", e);
      throw e;
    }
    try {
      measurementDataRepo.saveAllProteomics(proteomicsMeasurementsMapping);
    } catch (RuntimeException e) {
      log.error("Saving proteomics measurement in data repo failed", e);
      pxpMeasurementJpaRepo.deleteAll(proteomicsMeasurementsMapping.keySet());
      throw e;
    }
  }

  @Override
  public void saveAllNGS(
      Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping) {
    try {
      ngsMeasurementJpaRepo.saveAll(ngsMeasurementsMapping.keySet());
    } catch (RuntimeException e) {
      log.error("Saving ngs measurement failed", e);
      throw e;
    }
    try {
      measurementDataRepo.saveAllNGS(ngsMeasurementsMapping);
    } catch (RuntimeException e) {
      log.error("Saving ngs measurement in data repo failed", e);
      ngsMeasurementJpaRepo.deleteAll(ngsMeasurementsMapping.keySet());
      throw e;
    }
  }
}
