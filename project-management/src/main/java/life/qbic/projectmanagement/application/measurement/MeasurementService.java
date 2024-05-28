package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
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
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMethodMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
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
  private final ProjectInformationService projectInformationService;
  private final MeasurementRepository measurementRepository;

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService,
      OrganisationLookupService organisationLookupService,
      MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService,
      MeasurementRepository measurementRepository) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
    this.organisationLookupService = Objects.requireNonNull(organisationLookupService);
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
  }


  /**
   * Merges a collection of {@link NGSMeasurementMetadata} items into one single
   * {@link NGSMeasurementMetadata} item.
   * <p>
   * The method currently considers the sample codes as preservable.
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

  /**
   * Checks if there are measurements registered for the provided experimentId
   *
   * @param experimentId {@link ExperimentId}s of the experiment for which it should be determined
   *                     if its contained {@link Sample} have measurements attached
   * @return true if experiments has samples with associated measurements, false if not
   */
  public boolean hasMeasurements(ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.countMeasurementsBySampleIds(samplesInExperiment) != 0;
  }


  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<ProteomicsMeasurement> findProteomicsMeasurements(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder, ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryProteomicsMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, sortOrder);
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<ProteomicsMeasurement> findProteomicsMeasurements(ExperimentId experimentId,
      ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryAllProteomicsMeasurements(samplesInExperiment);
  }

  public Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementId) {
    return measurementLookupService.findProteomicsMeasurement(measurementId);
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
  public Collection<NGSMeasurement> findNGSMeasurements(ExperimentId experimentId,
      ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryAllNGSMeasurement(samplesInExperiment);
  }

  public Optional<NGSMeasurement> findNGSMeasurement(String measurementId) {
    return measurementLookupService.findNGSMeasurement(measurementId);
  }

  private Result<MeasurementId, ErrorCode> registerNGS(
      ProjectId projectId, NGSMeasurementMetadata metadata) {

    var associatedSampleCodes = metadata.associatedSample();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(metadata.associatedSample().code()));
    var sampleIdCodeEntries = queryIdCodePair(associatedSampleCodes);

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      return Result.fromError(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      return Result.fromError(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.sequencingReadType(), metadata.libraryKit(), metadata.flowCell(),
        metadata.sequencingRunProtocol(),
        metadata.indexI7(), metadata.indexI5());

    var measurement = NGSMeasurement.create(projectId,
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode, organisationQuery.get(), method, metadata.comment());

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurement::setSamplePoolGroup);

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addNGS(measurement, parentCodes);

    if (result.isError()) {
      return Result.fromError(ErrorCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  private Result<MeasurementId, ErrorCode> updateNGS(NGSMeasurementMetadata metadata) {
    var result = measurementLookupService.findNGSMeasurement(metadata.measurementId());
    if (result.isEmpty()) {
      return Result.fromError(ErrorCode.UNKNOWN_MEASUREMENT);
    }
    var measurementToUpdate = result.get();

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      return Result.fromError(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      return Result.fromError(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.sequencingReadType(),
        metadata.libraryKit(), metadata.flowCell(),
        metadata.sequencingRunProtocol(),
        metadata.indexI7(), metadata.indexI5());

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurementToUpdate::setSamplePoolGroup);

    measurementToUpdate.setMethod(method);
    measurementToUpdate.setComment(metadata.comment());
    var updateResult = measurementDomainService.updateNGS(measurementToUpdate);

    if (updateResult.isError()) {
      return Result.fromError(ErrorCode.FAILED);
    } else {
      return Result.fromValue(updateResult.getValue().measurementId());
    }
  }

  /**
   * Registers a collection of {@link MeasurementMetadata} items.
   * <p>
   * The method execution is transactional, the client can expect that either all measurements are
   * registered successfully or none is.
   * <p>
   * If there is at least one error or exception recorded, the complete transaction will be rolled
   * back.
   * <p>
   * If the returned collection {@link Result}s does not contain any error (equivalent to
   * {@link Result#isError()} == true), then the transaction was successful.
   *
   * @param measurementMetadataList a list of measurement metadata items to get registered
   * @param projectId               the project ID of the project the measurement should be
   *                                registered in
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  public CompletableFuture<List<Result<MeasurementId, ErrorCode>>> registerAll(
      List<MeasurementMetadata> measurementMetadataList, ProjectId projectId) {

    List<Result<MeasurementId, ErrorCode>> results;

    try {
      runPreRegistrationChecks(measurementMetadataList, projectId);
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    }
    try {
      results = performRegistration(measurementMetadataList, projectId).stream()
          .map(Result::<MeasurementId, ErrorCode>fromValue).toList();
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    } catch (RuntimeException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(ErrorCode.FAILED)));
    }

    return CompletableFuture.completedFuture(results);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Transactional
  protected List<MeasurementId> performRegistration(
      List<? extends MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    if (measurementMetadataList.isEmpty()) {
      return new ArrayList<>(); // Nothing to do
    }
    if (measurementMetadataList.get(0) instanceof ProteomicsMeasurementMetadata) {
      return performRegistrationPxp((List<MeasurementMetadata>) measurementMetadataList, projectId);

    }
    if (measurementMetadataList.get(0) instanceof NGSMeasurementMetadata) {
      return performRegistrationNGS((List<MeasurementMetadata>) measurementMetadataList, projectId);
    }
    throw new MeasurementRegistrationException(ErrorCode.FAILED);
  }

  private List<MeasurementId> performRegistrationNGS(
      List<MeasurementMetadata> measurementMetadataList,
      ProjectId projectId) {
    List<NGSMeasurementMetadata> ngsMeasurements = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof NGSMeasurementMetadata) {
        ngsMeasurements.add((NGSMeasurementMetadata) measurementMetadata);
      }
    }
    Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping = new HashMap<>();
    for (NGSMeasurementMetadata metadata : ngsMeasurements) {
      ngsMeasurementsMapping.putAll(prepareNGSMeasurement(projectId, metadata));
    }
    return measurementDomainService.addNGSAll(ngsMeasurementsMapping);
  }

  private Map<NGSMeasurement, Collection<SampleIdCodeEntry>> prepareNGSMeasurement(
      ProjectId projectId, NGSMeasurementMetadata metadata) {
    Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurements = new HashMap<>();
    var associatedSampleCodes = metadata.associatedSample();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(metadata.associatedSample().code()));
    var sampleIdCodeEntries = queryIdCodePair(associatedSampleCodes);

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.sequencingReadType(),
        metadata.libraryKit(), metadata.flowCell(), metadata.sequencingRunProtocol(),
        metadata.indexI7(), metadata.indexI5());

    var measurement = NGSMeasurement.create(
        projectId,
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        organisationQuery.get(),
        method, metadata.comment());

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurement::setSamplePoolGroup);
    ngsMeasurements.put(measurement, List.of(sampleIdCodeEntries.orElseThrow()));
    return ngsMeasurements;
  }

  private List<MeasurementId> performRegistrationPxp(
      List<MeasurementMetadata> measurementMetadataList,
      ProjectId projectId) {
    List<ProteomicsMeasurementMetadata> proteomicsMeasurements = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof ProteomicsMeasurementMetadata) {
        proteomicsMeasurements.add((ProteomicsMeasurementMetadata) measurementMetadata);
      }
    }
    Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping = new HashMap<>();

    // Start with the pooled measurements first and group the metadata entries by pool
    Map<String, List<ProteomicsMeasurementMetadata>> measurementsByPool = proteomicsMeasurements.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isPresent())
        .collect(Collectors.groupingBy(metadata -> metadata.assignedSamplePoolGroup().get()));

    // We collect the "single" sample measurements extra
    List<ProteomicsMeasurementMetadata> singleMeasurements = proteomicsMeasurements.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isEmpty()).toList();

    // Then merge and prepare the domain objects by pool
    proteomicsMeasurementsMapping.putAll(mergeByPool(measurementsByPool, projectId));
    // and last but not least also the single sample measurements
    singleMeasurements.stream()
        .map(singleMeasurement -> build(List.of(singleMeasurement), projectId))
        .forEach(proteomicsMeasurementsMapping::putAll);

    return measurementDomainService.addProteomicsAll(proteomicsMeasurementsMapping);
  }

  /**
   * Merges and builds {@link ProteomicsMeasurement} based on the given pool information.
   *
   * @param groupedMetadata already grouped measurement metadata by pool
   * @param projectId       the project the measurement belongs to
   * @return
   * @since 1.0.0
   */
  private Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> mergeByPool(
      Map<String, List<ProteomicsMeasurementMetadata>> groupedMetadata, ProjectId projectId) {
    Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> metadataMap = new HashMap<>();
    for (String poolLabel : groupedMetadata.keySet()) {
      var measurements = groupedMetadata.get(poolLabel);
      metadataMap.putAll(build(measurements, projectId));
    }
    return metadataMap;
  }

  private Map<SampleCode, SampleIdCodeEntry> buildSampleIdLookupTable(
      Collection<ProteomicsMeasurementMetadata> metadata) {
    Map<SampleCode, SampleIdCodeEntry> sampleIdLookupTable = new HashMap<>();
    var sampleCodes = metadata.stream().map(ProteomicsMeasurementMetadata::sampleCode).toList();
    for (SampleCode sampleCode : sampleCodes) {
      var sampleIdQueryResult = queryIdCodePair(sampleCode).orElseThrow();
      sampleIdLookupTable.put(sampleCode, sampleIdQueryResult);
    }
    return sampleIdLookupTable;
  }

  /**
   * Builds an instance of {@link ProteomicsMeasurement} and its corresponding
   * {@link SampleIdCodeEntry}s based on the given list of {@link ProteomicsMethodMetadata} and the
   * {@link ProjectId}.
   * <p>
   * Disclaimer: the method does NOT evaluate, of the entries belong together, e.g. they are part of
   * the same pool. This grouping needs to be done by the caller.
   *
   * @param metadataList A list of pre-grouped metadata of a pool or single measurement representing
   *                     one unit of a {@link ProteomicsMeasurement}.
   * @param projectId    the project the measurement belongs to
   * @return
   * @since 1.0.0
   */
  private Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> build(
      List<ProteomicsMeasurementMetadata> metadataList, ProjectId projectId) {
    Map<SampleCode, SampleIdCodeEntry> sampleIdLookupTable = buildSampleIdLookupTable(metadataList);
    var sampleCodes = sampleIdLookupTable.keySet();
    var specificMetadata = createSpecificMetadata(metadataList, sampleIdLookupTable);
    var assignedMeasurementCode = MeasurementCode.createMS(sampleCodes.iterator().next().code());
    var firstMetadataEntry = metadataList.get(0);

    var organisationQuery = organisationLookupService.organisation(
        firstMetadataEntry.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var instrumentQuery = resolveOntologyCURI(firstMetadataEntry.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), firstMetadataEntry.facility(),
        firstMetadataEntry.digestionMethod(), firstMetadataEntry.digestionEnzyme(),
        firstMetadataEntry.enrichmentMethod(), firstMetadataEntry.lcColumn(),
        firstMetadataEntry.lcmsMethod(), Integer.parseInt(firstMetadataEntry.injectionVolume()),
        firstMetadataEntry.labeling()
            .labelType());

    var measurement = ProteomicsMeasurement.create(projectId, assignedMeasurementCode,
        organisationQuery.get(), method, specificMetadata);

    measurement.setSamplePoolGroup(firstMetadataEntry.samplePoolGroup());

    return Map.of(measurement, sampleIdLookupTable.values());
  }

  private List<ProteomicsSpecificMeasurementMetadata> createSpecificMetadata(
      List<ProteomicsMeasurementMetadata> metadata,
      Map<SampleCode, SampleIdCodeEntry> sampleIdCodeLookupTable) {
    return metadata.stream().map(metadataEntry -> ProteomicsSpecificMeasurementMetadata.create(
        sampleIdCodeLookupTable.get(metadataEntry.associatedSample()).sampleId(),
        metadataEntry.labeling().label(), metadataEntry.fractionName(),
        metadataEntry.comment())).toList();
  }


  private void runPreRegistrationChecks(
      List<? extends MeasurementMetadata> measurements, ProjectId projectId)
      throws MeasurementRegistrationException {

    for (MeasurementMetadata measurementMetadata : measurements) {
      if (measurementMetadata.associatedSample() == null) {
        throw new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLE);
      }
      if (!isSampleFromProject(projectId, measurementMetadata.associatedSample())) {
        throw new MeasurementRegistrationException(ErrorCode.SAMPLECODE_NOT_FROM_PROJECT);
      }
      if (queryIdCodePair(measurementMetadata.associatedSample()).isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLE);
      }
    }
  }

  /**
   * Based on a list of measurement metadata entries with measurement IDs, the already registered
   * measurements are queried and updated.
   * <p>
   * The update happens async, so the client can continue by handling the {@link CompletableFuture}
   * object that is returned instantly.
   * <p>
   * The update happens atomic, and either was successful for all entries or none.
   * <p>
   * Possible reasons for a failing update are:
   *
   * <ul>
   *   <li>the measurement id does not belong to a registered entity</li>
   *   <li>some provided metadata ontologies or PIDs cannot be resolved</li>
   *   <li>some technical issue with the persistence layer</li>
   * </ul>
   * <p>
   * The client is advised to check for any result {@link Result#isError()} is present in the returned list.
   * <p>
   * Disclaimer: Pooled Measurements
   * Currently for pooled measurement updates, the pool group label is not updated to make the pool metadata handling less susceptible
   * to accidental errors.
   * However, the references of the samples pooled in the measurement have an impact and will lead to a reassignment of the associated measured samples if changed during the update.
   *
   * @param measurementMetadataList a list of measurements metadata to be updated
   * @param projectId               the project id of the project the measurements belong to
   * @return a {@link CompletableFuture} object with a list of {@link Result} objects, containing
   * either the measurement id of the updated measurement or an error code.
   * @since 1.0.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  public CompletableFuture<List<Result<MeasurementId, ErrorCode>>> updateAll(
      List<MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    List<Result<MeasurementId, ErrorCode>> results;

    if (!measurementMetadataList.isEmpty() && measurementMetadataList.get(
        0) instanceof ProteomicsMeasurementMetadata) {
      List<ProteomicsMeasurementMetadata> metadata = new ArrayList<>();
      for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
        metadata.add((ProteomicsMeasurementMetadata) measurementMetadata);
      }
      try {
        results = updateAllPxp(metadata, projectId);
      } catch (MeasurementRegistrationException e) {
        log.error("Measurement update failed.", e);
        return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
      }
      return CompletableFuture.completedFuture(results);
    }

    // Leave this for NGS legacy support, until pooling and multiplexing is solved with
    // domain experts (aka Morgana)
    var pooledMeasurements = mergeUpdatedBySamplePoolGroup(measurementMetadataList);
    try {
      results = performUpdate(pooledMeasurements, projectId);
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    }
    return CompletableFuture.completedFuture(results);
  }

  /**
   * Bulk update of a list of proteomics measurements.
   * <p>
   * This method takes care of two types of measurements:
   *
   * <ul>
   *   <li>single sample measurement</li>
   *   <li>pooled sample measurement</li>
   * </ul>
   * <p>
   * In the case of pooled sample measurement updates, the method groups
   * the metadata entries by measurement ID since the pool has already been registered and
   * cannot be reassigned.
   * <p>
   * In the current state of implementation, if a user wants to update the pool label, they have to
   * delete the measurement and register it properly again.
   *
   * @param metadata  the proteomics metadata to update
   * @param projectId the project the measurement belongs to
   * @return a list of {@link Result} objects
   * @since 1.0.0
   */
  private List<Result<MeasurementId, ErrorCode>> updateAllPxp(
      List<ProteomicsMeasurementMetadata> metadata, ProjectId projectId) {

    if (measurementCodeMissing(metadata)) {
      throw new MeasurementRegistrationException(ErrorCode.MISSING_MEASUREMENT_ID);
    }
    if (!allMeasurementCodesExist(
        metadata.stream().map(ProteomicsMeasurementMetadata::measurementId).toList())) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_MEASUREMENT);
    }

    var singleSampleMeasurements = metadata.stream()
        .filter(measurement -> measurement.assignedSamplePoolGroup().isEmpty()).toList();
    var pooledMeasurements = metadata.stream()
        .filter(measurement -> measurement.assignedSamplePoolGroup().isPresent()).collect(
            Collectors.groupingBy(ProteomicsMeasurementMetadata::measurementId));

    List<ProteomicsMeasurement> measurementsForUpdate = new ArrayList<>();

    var lookupTable = buildSampleIdLookupTable(metadata);

    for (ProteomicsMeasurementMetadata measurementMetadata : singleSampleMeasurements) {
      var measurement = measurementRepository.findProteomicsMeasurement(
          measurementMetadata.measurementId()).orElseThrow();
      measurement.setSpecificMetadata(
          createSpecificMetadata(List.of(measurementMetadata), lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          measurementMetadata.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var instrumentQuery = resolveOntologyCURI(measurementMetadata.instrumentCURI());
      if (instrumentQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new ProteomicsMethodMetadata(instrumentQuery.get(),
          measurementMetadata.facility(),
          measurementMetadata.digestionMethod(), measurementMetadata.digestionEnzyme(),
          measurementMetadata.enrichmentMethod(), measurementMetadata.lcColumn(),
          measurementMetadata.lcmsMethod(), Integer.parseInt(measurementMetadata.injectionVolume()),
          measurementMetadata.labeling()
              .labelType());

      measurement.setOrganisation(organisationQuery.get());
      measurement.setMethod(method);
      measurementsForUpdate.add(measurement);
    }

    for (String measurementId : pooledMeasurements.keySet()) {
      var pooledMeasurement = pooledMeasurements.get(measurementId);
      var firstEntry = pooledMeasurement.get(0);
      var measurement = measurementRepository.findProteomicsMeasurement(
          firstEntry.measurementId()).orElseThrow();
      measurement.setSpecificMetadata(createSpecificMetadata(pooledMeasurement, lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          firstEntry.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var instrumentQuery = resolveOntologyCURI(firstEntry.instrumentCURI());
      if (instrumentQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new ProteomicsMethodMetadata(instrumentQuery.get(), firstEntry.facility(),
          firstEntry.digestionMethod(), firstEntry.digestionEnzyme(),
          firstEntry.enrichmentMethod(), firstEntry.lcColumn(),
          firstEntry.lcmsMethod(), Integer.parseInt(firstEntry.injectionVolume()),
          firstEntry.labeling()
              .labelType());

      measurement.setOrganisation(organisationQuery.get());
      measurement.setMethod(method);
      measurementsForUpdate.add(measurement);
    }

    try {
      var ids = measurementDomainService.updateProteomicsAll(measurementsForUpdate);
      return ids.stream().map(Result::<MeasurementId, ErrorCode>fromValue).toList();
    } catch (RuntimeException e) {
      return List.of(Result.fromError(ErrorCode.FAILED));
    }
  }

  private boolean measurementCodeMissing(List<ProteomicsMeasurementMetadata> metadata) {
    return metadata.stream().anyMatch(entry -> entry.measurementId().isBlank());
  }

  private boolean allMeasurementCodesExist(List<String> measurementCode) {
    return measurementCode.stream().map(measurementRepository::findProteomicsMeasurement)
        .noneMatch(Optional::isEmpty);
  }

  /**
   * In an edit context, a measurement with a pooled sample group appears multiple times (once per
   * row per sample). Since the user cannot change the sample group of a measurement within the edit
   * context, we assume that the provided values for the first row of the pooled measurement are the
   * ones were interested and discard the rest.
   */
  private List<MeasurementMetadata> mergeUpdatedBySamplePoolGroup(
      List<MeasurementMetadata> measurementMetadata) {
    return measurementMetadata.stream().distinct().toList();
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Transactional
  protected List<Result<MeasurementId, ErrorCode>> performUpdate(
      List<? extends MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    if (measurementMetadataList.isEmpty()) {
      return new ArrayList<>(); // Nothing to do
    }
    try {
      if (measurementMetadataList.get(0) instanceof NGSMeasurementMetadata) {
        return performUpdateNGS(measurementMetadataList, projectId);
      }
    } catch (Exception exception) {
      throw new MeasurementRegistrationException(ErrorCode.FAILED);
    }

    throw new MeasurementRegistrationException(ErrorCode.FAILED);
  }

  private List<Result<MeasurementId, ErrorCode>> performUpdateNGS(
      List<? extends MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    List<NGSMeasurementMetadata> ngsMeasurements = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof NGSMeasurementMetadata) {
        ngsMeasurements.add((NGSMeasurementMetadata) measurementMetadata);
      }
    }
    return measurementDomainService.updateNGSAll(
            ngsMeasurements.stream().map(this::prepareNGSMeasurementUpdate).toList()).stream()
        .map(Result::<MeasurementId, ErrorCode>fromValue).toList();
  }

  private NGSMeasurement prepareNGSMeasurementUpdate(NGSMeasurementMetadata metadata) {
    var result = measurementLookupService.findNGSMeasurement(metadata.measurementId());
    if (result.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_MEASUREMENT);
    }
    var measurementToUpdate = result.get();

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.sequencingReadType(),
        metadata.libraryKit(),
        metadata.flowCell(), metadata.sequencingRunProtocol(),
        metadata.indexI7(), metadata.indexI5());

    measurementToUpdate.setComment(metadata.comment());
    measurementToUpdate.setSamplePoolGroup(metadata.samplePoolGroup());
    measurementToUpdate.setMethod(method);
    return measurementToUpdate;
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

  private Optional<SampleIdCodeEntry> queryIdCodePair(SampleCode sampleCode) {
    return sampleInformationService.findSampleId(sampleCode);
  }

  /*Ensures that the provided sample code belong to one of the experiments within the project*/
  private boolean isSampleFromProject(ProjectId projectId, SampleCode sampleCodes) {
    var sampleIdQueryResult = sampleInformationService.findSampleId(sampleCodes);

    if (sampleIdQueryResult.isEmpty()) {
      return false;
    }

    var sampleId = sampleIdQueryResult.map(SampleIdCodeEntry::sampleId).orElseThrow();

    var samples = sampleInformationService.retrieveSamplesByIds(List.of(sampleId));
    var associatedExperimentsFromSamples = samples.stream().map(Sample::experimentId).toList();

    var associatedExperimentsFromProject = projectInformationService.find(projectId).orElseThrow()
        .experiments();
    return new HashSet<>(associatedExperimentsFromProject).containsAll(
        associatedExperimentsFromSamples);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<Void, MeasurementDeletionException> deletePtxMeasurements(ProjectId projectId,
      Set<ProteomicsMeasurement> selectedMeasurements) {
    try {
      measurementDomainService.deletePtx(selectedMeasurements);
      return Result.fromValue(null);
    } catch (MeasurementDeletionException e) {
      return Result.fromError(e);
    }
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<Void, MeasurementDeletionException> deleteNGSMeasurements(ProjectId projectId,
      Set<NGSMeasurement> selectedMeasurements) {
    try {
      measurementDomainService.deleteNGS(selectedMeasurements);
      return Result.fromValue(null);
    } catch (MeasurementDeletionException e) {
      return Result.fromError(e);
    }
  }

  public enum DeletionErrorCode {
    FAILED, DATA_ATTACHED
  }

  public enum ErrorCode {
    FAILED, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, WRONG_EXPERIMENT, MISSING_ASSOCIATED_SAMPLE, MISSING_MEASUREMENT_ID, SAMPLECODE_NOT_FROM_PROJECT, UNKNOWN_MEASUREMENT
  }

  public static final class MeasurementDeletionException extends RuntimeException {

    private final DeletionErrorCode reason;

    public MeasurementDeletionException(DeletionErrorCode reason) {
      this.reason = reason;
    }

    public DeletionErrorCode reason() {
      return reason;
    }
  }

  public static final class MeasurementRegistrationException extends RuntimeException {

    private final ErrorCode reason;

    public MeasurementRegistrationException(ErrorCode reason) {
      this.reason = reason;
    }

    public ErrorCode reason() {
      return reason;
    }
  }

  private record MeasurementUpdatedDomainEventSubscriber(
      List<DomainEvent> domainEventsCache) implements
      DomainEventSubscriber<DomainEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return MeasurementUpdatedEvent.class;
    }

    @Override
    public void handleEvent(DomainEvent event) {
      domainEventsCache.add(event);
    }
  }
}
