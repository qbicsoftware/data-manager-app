package life.qbic.projectmanagement.application.measurement;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.ImmunopeptidomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
  private final IpMeasurementLookup ipMeasurementLookup;

  public MeasurementLookupService(@Autowired MeasurementRepository measurementRepository,
      NgsMeasurementLookup ngsMeasurementLookup, PxpMeasurementLookup pxpMeasurementLookup,
      IpMeasurementLookup ipMeasurementLookup) {
    this.ngsMeasurementLookup = ngsMeasurementLookup;
    this.pxpMeasurementLookup = pxpMeasurementLookup;
    this.ipMeasurementLookup = ipMeasurementLookup;
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

  public Optional<ImmunopeptidomicsMeasurement> findIPMeasurement(String measurementCode) {
    return measurementRepository.findIPMeasurement(measurementCode);
  }

  public Optional<ImmunopeptidomicsMeasurement> findIPMeasurementById(String measurementId) {
    return measurementRepository.findIPMeasurementById(measurementId);
  }

  public int countMeasurements(ProjectId projectId, ExperimentId experimentId) {
    var ngsFilter = NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value());
    var pxpFilter = PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value());
    var ipFilter = IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value());
    return ngsMeasurementLookup.countNgsMeasurements(projectId.value(), ngsFilter)
        + pxpMeasurementLookup.countPxpMeasurements(projectId.value(), pxpFilter)
        + ipMeasurementLookup.countIpMeasurements(projectId.value(), ipFilter);
  }

  /**
   * Returns the subset of {@code sampleIds} that are referenced by at least one measurement (NGS,
   * PXP, or IP) for the given experiment.
   *
   * @param projectId    the project the samples belong to
   * @param experimentId the experiment the samples belong to
   * @param sampleIds    the sample identifiers to check
   * @return the sample identifiers that have an associated measurement
   */
  public Set<String> findMeasuredSampleIds(ProjectId projectId, ExperimentId experimentId,
      Set<String> sampleIds) {
    var measuredSampleIds = new HashSet<String>();
    String projectIdValue = projectId.value();

    var ngsFilter = NgsMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value())
        .includingSamples(sampleIds);
    ngsMeasurementLookup.lookupNgsMeasurements(projectIdValue, 0, Integer.MAX_VALUE,
            Sort.unsorted(), ngsFilter)
        .forEach(info -> info.sampleInfos().forEach(sampleInfo -> measuredSampleIds.add(
            sampleInfo.sampleId())));

    var pxpFilter = PxpMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value())
        .includingSamples(sampleIds);
    pxpMeasurementLookup.lookupPxpMeasurements(projectIdValue, 0, Integer.MAX_VALUE,
            Sort.unsorted(), pxpFilter)
        .forEach(info -> info.sampleInfos().forEach(sampleInfo -> measuredSampleIds.add(
            sampleInfo.sampleId())));

    var ipFilter = IpMeasurementLookup.MeasurementFilter.forExperiment(experimentId.value())
        .includingSamples(sampleIds);
    ipMeasurementLookup.lookupIpMeasurements(projectIdValue, 0, Integer.MAX_VALUE,
            Sort.unsorted(), ipFilter)
        .forEach(info -> info.sampleInfos().forEach(sampleInfo -> measuredSampleIds.add(
            sampleInfo.sampleId())));

    measuredSampleIds.retainAll(sampleIds);
    return measuredSampleIds;
  }
}
