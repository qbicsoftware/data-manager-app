package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationLookupService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsLabeling;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMethodMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSamplePreparation;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Measurement Service
 * <p>
 * Service that provides an API to manage and query measurement information
 */
@Service
public class MeasurementService {

  private static final Logger log = logger(MeasurementService.class);
  private final MeasurementDomainService measurementDomainService;
  private final MeasurementLookupService measurementLookupService;
  private final SampleInformationService sampleInformationService;
  private final OntologyLookupService ontologyLookupService;
  private final OrganisationLookupService organisationLookupService;

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService,
      OrganisationLookupService organisationLookupService,
      MeasurementLookupService measurementLookupService) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
    this.organisationLookupService = Objects.requireNonNull(organisationLookupService);
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
  }

  private static Optional<ProteomicsMeasurementMetadata> merge(
      List<ProteomicsMeasurementMetadata> measurementMetadataList) {
    if (measurementMetadataList.isEmpty()) {
      return Optional.empty();
    }
    List<SampleCode> associatedSamples = measurementMetadataList.stream().map(
        ProteomicsMeasurementMetadata::sampleCodes).flatMap(Collection::stream).toList();
    var firstEntry = measurementMetadataList.get(0);
    return Optional.of(
        ProteomicsMeasurementMetadata.copyWithNewSamples(associatedSamples, firstEntry));
  }


  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<NGSMeasurement> findNGSMeasurements(String filter, ExperimentId experimentId,
      int offset,
      int limit,
      List<SortOrder> sortOrders, ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryNGSMeasurementsBySampleIds(filter, samplesInExperiment,
        offset, limit, sortOrders);
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<ProteomicsMeasurement> findProteomicsMeasurement(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryProteomicsMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, sortOrder);
  }

  private Result<MeasurementId, ResponseCode> registerNGS(
      NGSMeasurementMetadata ngsMeasurementMetadata) {

    var associatedSampleCodes = ngsMeasurementMetadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(ngsMeasurementMetadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      return Result.fromError(ResponseCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(ngsMeasurementMetadata.instrumentCURIE());
    if (instrumentQuery.isEmpty()) {
      return Result.fromError(ResponseCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var measurement = NGSMeasurement.create(
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        instrumentQuery.get());

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addNGS(measurement, parentCodes);

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  private Result<MeasurementId, ResponseCode> registerPxP(
      ProteomicsMeasurementMetadata metadata) {
    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createMS(
        String.valueOf(metadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      return Result.fromError(ResponseCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      return Result.fromError(ResponseCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      return Result.fromError(ResponseCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.fractionName(),
        metadata.digestionMethod(), metadata.digestionEnzyme(),
        metadata.enrichmentMethod(), Integer.parseInt(metadata.injectionVolume()),
        metadata.lcColumn(), metadata.lcmsMethod());

    var samplePreparation = new ProteomicsSamplePreparation(metadata.comment());
    var labelingMethod = new ProteomicsLabeling(metadata.labelingType(), metadata.label());

    var measurement = ProteomicsMeasurement.create(
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        organisationQuery.get(),
        method, samplePreparation);

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurement::setSamplePoolGroup);

    measurement.setLabeling(labelingMethod);

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addProteomics(measurement, parentCodes);

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<MeasurementId, ResponseCode> register(ProjectId projectId,
      MeasurementMetadata measurementMetadata) {
    if (measurementMetadata.associatedSamples().isEmpty()) {
      return Result.fromError(ResponseCode.MISSING_ASSOCIATED_SAMPLES);
    }
    if (measurementMetadata instanceof ProteomicsMeasurementMetadata proteomicsMeasurementMetadata) {
      return registerPxP(proteomicsMeasurementMetadata);
    }
    if (measurementMetadata instanceof NGSMeasurementMetadata ngsMeasurementMetadata) {
      return registerNGS(ngsMeasurementMetadata);
    }
    return Result.fromError(ResponseCode.FAILED);
  }

  @Transactional
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void registerMultiple(
      List<MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    var mergedSamplePoolGroups = mergeBySamplePoolGroup(measurementMetadataList);
    for (MeasurementMetadata measurementMetadata : mergedSamplePoolGroups) {
      register(projectId, measurementMetadata)
          .onError(error -> {
            throw new MeasurementRegistrationException(error);
          });
    }
  }

  private List<? extends MeasurementMetadata> mergeBySamplePoolGroup(
      List<? extends MeasurementMetadata> measurementMetadataList) {
    if (measurementMetadataList.isEmpty()) {
      return measurementMetadataList;
    }
    if (measurementMetadataList.stream()
        .allMatch(ProteomicsMeasurementMetadata.class::isInstance)) {
      var proteomicsMeasurementMetadataList = measurementMetadataList.stream()
          .map(ProteomicsMeasurementMetadata.class::cast).toList();
      return mergeBySamplePoolGroupProteomics(proteomicsMeasurementMetadataList);
    } else {
      throw new RuntimeException(
          "Merging measurement metadata: expected proteomics metadata only.");
    }
  }

  private List<ProteomicsMeasurementMetadata> mergeBySamplePoolGroupProteomics(
      List<ProteomicsMeasurementMetadata> proteomicsMeasurementMetadataList) {
    var poolingGroups = proteomicsMeasurementMetadataList.stream().filter(
        proteomicsMeasurementMetadata -> proteomicsMeasurementMetadata.assignedSamplePoolGroup()
            .isPresent()).collect(Collectors.groupingBy(
        metadata -> metadata.assignedSamplePoolGroup().orElseThrow()));
    List<ProteomicsMeasurementMetadata> mergedPooledMeasurements = poolingGroups.values().stream()
        .map(MeasurementService::merge).filter(Optional::isPresent).map(Optional::get).toList();

    var singleMeasurements = proteomicsMeasurementMetadataList.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isEmpty()).toList();

    return Stream.concat(singleMeasurements.stream(),
        mergedPooledMeasurements.stream()).toList();
  }

  private Optional<OntologyTerm> resolveOntologyCURI(String ontologyCURI) {
    return ontologyLookupService.findByCURI(ontologyCURI).map(OntologyTerm::from);
  }

  private Collection<SampleIdCodeEntry> queryIdCodePairs(Collection<SampleCode> sampleCodes) {
    return sampleCodes.stream().map(sampleInformationService::findSampleId)
        .filter(Optional::isPresent)
        .map(Optional::get).toList();
  }

  public enum ResponseCode {
    FAILED, SUCCESSFUL, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, WRONG_EXPERIMENT, MISSING_ASSOCIATED_SAMPLES
  }

  public static final class MeasurementRegistrationException extends RuntimeException {

    private final MeasurementService.ResponseCode reason;

    public MeasurementRegistrationException(ResponseCode reason) {
      this.reason = reason;
    }

    public ResponseCode reason() {
      return reason;
    }
  }

}
