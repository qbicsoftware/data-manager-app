package life.qbic.projectmanagement.application.measurement;

import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MeasurementLookupService
 * <p>
 * Service that provides an API to search and filter measurement information
 */
@Service
public class MeasurementLookupService {
  private final MeasurementRepository measurementRepository;
  private final NgsMeasurementLookup ngsMeasurementLookup;
  private final PxpMeasurementLookup pxpMeasurementLookup;

  public MeasurementLookupService(@Autowired MeasurementRepository measurementRepository,
      NgsMeasurementLookup ngsMeasurementLookup, PxpMeasurementLookup pxpMeasurementLookup) {
    this.ngsMeasurementLookup = ngsMeasurementLookup;
    this.pxpMeasurementLookup = pxpMeasurementLookup;
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }

  public Optional<ProteomicsMeasurement> findProteomicsMeasurementById(String measurementId) {
    return measurementRepository.findProteomicsMeasurementById(measurementId);
  }

  public Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementCode) {
    return measurementRepository.findProteomicsMeasurement(measurementCode);
  }

  public Optional<NGSMeasurement> findNGSMeasurementById(String measurementId) {
    return measurementRepository.findNGSMeasurementById(measurementId);
  }

  public Optional<NGSMeasurement> findNGSMeasurement(String measurementId) {
    return measurementRepository.findNGSMeasurement(measurementId);
  }


  public int countMeasurements(ProjectId projectId, ExperimentId experimentId) {
    var ngsFilter = NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value());
    var pxpFilter = PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value());
    return ngsMeasurementLookup.countNgsMeasurements(projectId.value(), ngsFilter)
        + pxpMeasurementLookup.countPxpMeasurements(projectId.value(), pxpFilter);
  }
}
