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
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
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
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService,
      OrganisationLookupService organisationLookupService,
      MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService) {
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
        life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata.copyWithNewProperties(
            associatedSamples, labels,
            firstEntry));
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
  public long countProteomicsMeasurements(ExperimentId experimentId,
      ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.countProteomicsMeasurementsBySampleIds(samplesInExperiment);
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
  public long countNGSMeasurements(ExperimentId experimentId, ProjectId projectId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.countNGSMeasurementsBySampleIds(samplesInExperiment);
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

    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(metadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      return Result.fromError(ErrorCode.FAILED);
    }

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

  private Result<MeasurementId, ErrorCode> registerPxP(
      ProjectId projectId, ProteomicsMeasurementMetadata metadata) {
    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createMS(
        String.valueOf(metadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      throw new MeasurementRegistrationException(ErrorCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
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
      throw new MeasurementRegistrationException(ErrorCode.FAILED);
    }
    return Result.fromValue(result.getValue().measurementId());
  }

  @Transactional
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public Result<MeasurementId, ErrorCode> update(ProjectId projectId,
      MeasurementMetadata metadata) {
    if (metadata.measurementIdentifier().isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.MISSING_MEASUREMENT_ID);
    }
    if (!areSamplesFromProject(projectId, metadata.associatedSamples())) {
      throw new MeasurementRegistrationException(ErrorCode.SAMPLECODE_NOT_FROM_PROJECT);
    }
    if (metadata instanceof ProteomicsMeasurementMetadata pxpMetadata) {
      return updatePxP(pxpMetadata);
    }
    if (metadata instanceof NGSMeasurementMetadata ngsMeasurementMetadata) {
      return updateNGS(ngsMeasurementMetadata);
    }
    throw new MeasurementRegistrationException(ErrorCode.FAILED);
  }

  private Result<MeasurementId, ErrorCode> updatePxP(ProteomicsMeasurementMetadata metadata) {
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    LocalDomainEventDispatcher.instance().subscribe(
        new MeasurementUpdatedDomainEventSubscriber(domainEventsCache));

    var result = measurementLookupService.findProteomicsMeasurement(metadata.measurementId());
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

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.fractionName(),
        metadata.digestionMethod(), metadata.digestionEnzyme(),
        metadata.enrichmentMethod(), Integer.parseInt(metadata.injectionVolume()),
        metadata.lcColumn(), metadata.lcmsMethod());

    var samplePreparation = new ProteomicsSamplePreparation(metadata.comment());
    var labelingMethod = metadata.labeling().stream().map(label -> new ProteomicsLabeling(
        label.sampleCode(), label.labelType(), label.label())).collect(Collectors.toList());

    measurementToUpdate.setSamplePreparation(samplePreparation);
    measurementToUpdate.setLabeling(labelingMethod);

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurementToUpdate::setSamplePoolGroup);

    measurementToUpdate.setLabeling(labelingMethod);
    measurementToUpdate.setMethod(method);
    measurementToUpdate.setComment(metadata.comment());

    var updateResult = measurementDomainService.updateProteomics(measurementToUpdate);

    if (updateResult.isError()) {
      return Result.fromError(ErrorCode.FAILED);
    } else {
      domainEventsCache.forEach(
          domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));
      return Result.fromValue(updateResult.getValue().measurementId());
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

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Transactional
  protected Result<MeasurementId, ErrorCode> register(ProjectId projectId,
      MeasurementMetadata measurementMetadata) {
    if (measurementMetadata.associatedSamples().isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLES);
    }
    if (!areSamplesFromProject(projectId, measurementMetadata.associatedSamples())) {
      throw new MeasurementRegistrationException(ErrorCode.SAMPLECODE_NOT_FROM_PROJECT);
    }
    if (measurementMetadata instanceof ProteomicsMeasurementMetadata proteomicsMeasurementMetadata) {
      return registerPxP(projectId, proteomicsMeasurementMetadata);
    }
    if (measurementMetadata instanceof NGSMeasurementMetadata ngsMeasurementMetadata) {
      return registerNGS(projectId, ngsMeasurementMetadata);
    }
    throw new MeasurementRegistrationException(ErrorCode.FAILED);
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
    var mergedSamplePoolGroups = mergeRegisteredBySamplePoolGroup(measurementMetadataList);
    List<Result<MeasurementId, ErrorCode>> results;

    try {
      runPreRegistrationChecks(mergedSamplePoolGroups, projectId);
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    }
    try {
      results = performRegistration(mergedSamplePoolGroups, projectId).stream()
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
    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(metadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error(
          "Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      throw new MeasurementRegistrationException(ErrorCode.FAILED);
    }

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
    ngsMeasurements.put(measurement, sampleIdCodeEntries);
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
    for (ProteomicsMeasurementMetadata metadata : proteomicsMeasurements) {
      proteomicsMeasurementsMapping.putAll(preparePxpMeasurement(projectId, metadata));
    }
    return measurementDomainService.addProteomicsAll(proteomicsMeasurementsMapping);
  }

  private Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> preparePxpMeasurement(
      ProjectId projectId, ProteomicsMeasurementMetadata metadata) {
    Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurements = new HashMap<>();
    var associatedSampleCodes = metadata.associatedSamples();
    var selectedSampleCode = MeasurementCode.createMS(
        String.valueOf(metadata.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error(
          "Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      throw new MeasurementRegistrationException(ErrorCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(metadata.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        metadata.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
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
    proteomicsMeasurements.put(measurement, sampleIdCodeEntries);
    return proteomicsMeasurements;
  }

  private void runPreRegistrationChecks(
      List<? extends MeasurementMetadata> mergedSamplePoolGroups, ProjectId projectId)
      throws MeasurementRegistrationException {

    for (MeasurementMetadata measurementMetadata : mergedSamplePoolGroups) {
      if (measurementMetadata.associatedSamples().isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLES);
      }
      if (!areSamplesFromProject(projectId, measurementMetadata.associatedSamples())) {
        throw new MeasurementRegistrationException(ErrorCode.SAMPLECODE_NOT_FROM_PROJECT);
      }
      if (queryIdCodePairs(measurementMetadata.associatedSamples()).size()
          != measurementMetadata.associatedSamples().size()) {
        throw new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLES);
      }
    }
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  public CompletableFuture<List<Result<MeasurementId, ErrorCode>>> updateAll(
      List<MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    List<Result<MeasurementId, ErrorCode>> results;

    var pooledMeasurements = mergeUpdatedBySamplePoolGroup(measurementMetadataList);
    try {
      results = performUpdate(pooledMeasurements, projectId);
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    }
    return CompletableFuture.completedFuture(results);
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
      if (measurementMetadataList.get(0) instanceof ProteomicsMeasurementMetadata) {
        return performUpdatePxp(measurementMetadataList, projectId);
      }
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

  private List<Result<MeasurementId, ErrorCode>> performUpdatePxp(
      List<? extends MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    List<ProteomicsMeasurementMetadata> proteomicsMeasurements = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof ProteomicsMeasurementMetadata) {
        proteomicsMeasurements.add((ProteomicsMeasurementMetadata) measurementMetadata);
      }
    }
    return measurementDomainService.updateProteomicsAll(
            proteomicsMeasurements.stream().map(this::preparePxpMeasurementUpdate).toList()).stream()
        .map(Result::<MeasurementId, ErrorCode>fromValue).toList();
  }

  private ProteomicsMeasurement preparePxpMeasurementUpdate(
      ProteomicsMeasurementMetadata metadata) {
    var result = measurementLookupService.findProteomicsMeasurement(metadata.measurementId());
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

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), metadata.facility(),
        metadata.fractionName(),
        metadata.digestionMethod(), metadata.digestionEnzyme(),
        metadata.enrichmentMethod(), Integer.parseInt(metadata.injectionVolume()),
        metadata.lcColumn(), metadata.lcmsMethod());

    var samplePreparation = new ProteomicsSamplePreparation(metadata.comment());
    var labelingMethod = metadata.labeling().stream().map(label -> new ProteomicsLabeling(
        label.sampleCode(), label.labelType(), label.label())).collect(Collectors.toList());

    measurementToUpdate.setSamplePreparation(samplePreparation);
    measurementToUpdate.setLabeling(labelingMethod);

    metadata.assignedSamplePoolGroup()
        .ifPresent(measurementToUpdate::setSamplePoolGroup);

    measurementToUpdate.setLabeling(labelingMethod);
    measurementToUpdate.setMethod(method);

    return measurementToUpdate;
  }


  private List<? extends MeasurementMetadata> mergeRegisteredBySamplePoolGroup(
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
          "Merging measurement metadata: expected proteomics or ngs metadata only.");
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
  private boolean areSamplesFromProject(ProjectId projectId, List<SampleCode> sampleCodes) {
    var possibleSampleIds = sampleCodes.stream().map(sampleInformationService::findSampleId)
        .toList();
    //If an invalid sampleCode was provided we fail early
    if (possibleSampleIds.stream().anyMatch(Optional::isEmpty)) {
      return false;
    }
    var sampleIds = possibleSampleIds.stream().map(Optional::get).map(SampleIdCodeEntry::sampleId)
        .toList();
    var samples = sampleInformationService.retrieveSamplesByIds(sampleIds);
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
    FAILED, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, WRONG_EXPERIMENT, MISSING_ASSOCIATED_SAMPLES, MISSING_MEASUREMENT_ID, SAMPLECODE_NOT_FROM_PROJECT, UNKNOWN_MEASUREMENT
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
