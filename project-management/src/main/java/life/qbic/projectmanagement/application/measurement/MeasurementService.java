package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationLookupService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSMethodMetadata;
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
public class MeasurementService{

  private static final Logger log = logger(MeasurementService.class);
  private final MeasurementDomainService measurementDomainService;
  private final MeasurementLookupService measurementLookupService;
  private final SampleInformationService sampleInformationService;
  private final OntologyLookupService ontologyLookupService;
  private final OrganisationLookupService organisationLookupService;
  private final ProjectInformationService projectInformationService;

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService,
      OrganisationLookupService organisationLookupService,
      MeasurementLookupService measurementLookupService, ProjectInformationService projectInformationService) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
    this.organisationLookupService = Objects.requireNonNull(organisationLookupService);
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
  }

  /**
   * Merges a collection of {@link ProteomicsMeasurementMetadata} items into one single
   * {@link ProteomicsMeasurementMetadata} item.
   * <p>
   * The method currently considers labels to be distinctly preserved, as well as the sample codes.
   * <p>
   * For all other properties, there is no guarantee from which item they are derived.
   *
   * @param metadata a collection of metadata items to be merged into a single item
   * @return
   * @since 1.0.0
   */
  private static Optional<ProteomicsMeasurementMetadata> mergePxP(
      Collection<ProteomicsMeasurementMetadata> metadata) {
    if (metadata.isEmpty()) {
      return Optional.empty();
    }
    List<SampleCode> associatedSamples = metadata.stream().map(
        ProteomicsMeasurementMetadata::sampleCodes).flatMap(Collection::stream).toList();
    var labels = metadata.stream().flatMap(theMetadata -> theMetadata.labeling().stream())
        .collect(
            Collectors.toSet());
    var firstEntry = metadata.iterator().next();
    return Optional.of(
        ProteomicsMeasurementMetadata.copyWithNewProperties(associatedSamples, labels,
            firstEntry));
  }

  /**
   * Merges a collection of {@link NGSMeasurementMetadata} items into one single
   * {@link NGSMeasurementMetadata} item.
   * <p>
   * The method currently considers labels to be distinctly preserved, as well as the sample codes.
   * <p>
   * For all other properties, there is no guarantee from which item they are derived.
   *
   * @param metadata a collection of metadata items to be merged into a single item
   * @return
   */
  private static Optional<NGSMeasurementMetadata> mergeNGS(
      Collection<NGSMeasurementMetadata> metadata) {
    if (metadata.isEmpty()) {
      return Optional.empty();
    }
    List<SampleCode> associatedSamples = metadata.stream().map(
        NGSMeasurementMetadata::sampleCodes).flatMap(Collection::stream).toList();
    var indexI7 = metadata.stream().map(NGSMeasurementMetadata::indexI7).findFirst().orElseThrow();
    var indexI5 = metadata.stream().map(NGSMeasurementMetadata::indexI5).findFirst().orElseThrow();
    var firstEntry = metadata.iterator().next();
    return Optional.of(
        NGSMeasurementMetadata.copyWithNewProperties(associatedSamples, indexI7, indexI5,
            firstEntry));
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
      ProjectId projectId, NGSMeasurementMetadata metadata) {

    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
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

    var method = new NGSMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.sequencingReadType(), metadata.libraryKit(), metadata.flowCell(),
        metadata.sequencingRunProtocol(),
        metadata.indexI7(), metadata.indexI5());

    var measurement = NGSMeasurement.create(projectId,
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode, organisationQuery.get(), method, metadata.comment());

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addNGS(measurement, parentCodes);

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  private Result<MeasurementId, ResponseCode> registerPxP(
      ProjectId projectId, ProteomicsMeasurementMetadata metadata) {
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
    var labelingMethod = metadata.labeling().stream().map(label -> new ProteomicsLabeling(
        label.sampleCode(), label.labelType(), label.label())).toList();

    var measurement = ProteomicsMeasurement.create(
        projectId,
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        organisationQuery.get(),
        method, samplePreparation);

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurement::setSamplePoolGroup);

    measurement.setLabeling(labelingMethod);

    measurement.setFraction(metadata.fractionName());

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
    if(!areSamplesFromProject(projectId, measurementMetadata.associatedSamples())){
      return Result.fromError(ResponseCode.SAMPLECODE_NOT_FROM_PROJECT);
    }
    if (measurementMetadata instanceof ProteomicsMeasurementMetadata proteomicsMeasurementMetadata) {
      return registerPxP(projectId, proteomicsMeasurementMetadata);
    }
    if (measurementMetadata instanceof NGSMeasurementMetadata ngsMeasurementMetadata) {
      return registerNGS(projectId, ngsMeasurementMetadata);
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
        .allMatch(NGSMeasurementMetadata.class::isInstance)) {
      var ngsMeasurementMetadataList = measurementMetadataList.stream()
          .map(NGSMeasurementMetadata.class::cast).toList();
      return mergeBySamplePoolGroupNGS(ngsMeasurementMetadataList);
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
        .map(MeasurementService::mergePxP).filter(Optional::isPresent).map(Optional::get).toList();

    var singleMeasurements = proteomicsMeasurementMetadataList.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isEmpty()).toList();

    return Stream.concat(singleMeasurements.stream(),
        mergedPooledMeasurements.stream()).toList();
  }

  private List<NGSMeasurementMetadata> mergeBySamplePoolGroupNGS(
      List<NGSMeasurementMetadata> ngsMeasurementMetadataList) {
    var poolingGroups = ngsMeasurementMetadataList.stream().filter(
        ngsMeasurementMetadata -> ngsMeasurementMetadata.assignedSamplePoolGroup()
            .isPresent()).collect(Collectors.groupingBy(
        metadata -> metadata.assignedSamplePoolGroup().orElseThrow()));
    List<NGSMeasurementMetadata> mergedPooledMeasurements = poolingGroups.values().stream()
        .map(MeasurementService::mergeNGS).filter(Optional::isPresent).map(Optional::get).toList();

    var singleMeasurements = ngsMeasurementMetadataList.stream()
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

  /*Ensures that the provided sample code belong to one of the experiments within the project*/
  private boolean areSamplesFromProject(ProjectId projectId, List<SampleCode> sampleCodes){
    var possibleSampleIds = sampleCodes.stream().map(sampleInformationService::findSampleId).toList();
    //If an invalid sampleCode was provided we fail early
    if(possibleSampleIds.stream().anyMatch(Optional::isEmpty)){
      return false;
    }
    var sampleIds = possibleSampleIds.stream().map(Optional::get).map(SampleIdCodeEntry::sampleId).toList();
    var samples = sampleInformationService.retrieveSamplesByIds(sampleIds);
    var associatedExperimentsFromSamples = samples.stream().map(Sample::experimentId).toList();
    var associatedExperimentsFromProject = projectInformationService.find(projectId).orElseThrow().experiments();
    return new HashSet<>(associatedExperimentsFromProject).containsAll(associatedExperimentsFromSamples);
  }

  public enum ResponseCode {
    FAILED, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, SAMPLECODE_NOT_FROM_PROJECT, MISSING_ASSOCIATED_SAMPLES
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
