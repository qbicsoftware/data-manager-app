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
import life.qbic.application.commons.Result;
import life.qbic.application.commons.SortOrder;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.LocalDomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationLookupService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementSpecificPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementUpdateInformationPxP;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSMethodMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMethodMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementCreatedEvent;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.repository.MeasurementRepository;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.NonNull;


/**
 * Measurement Service
 * <p>
 * Service that provides an API to manage and search measurement information
 */
@Service
public class MeasurementService {

  private static final Logger log = logger(MeasurementService.class);
  private final ApplicationContext context;
  private final MeasurementDomainService measurementDomainService;
  private final MeasurementLookupService measurementLookupService;
  private final SampleInformationService sampleInformationService;
  private final OrganisationLookupService organisationLookupService;
  private final ProjectInformationService projectInformationService;
  private final MeasurementRepository measurementRepository;
  private final TerminologyService terminologyService;

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OrganisationLookupService organisationLookupService,
      MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService,
      MeasurementRepository measurementRepository,
      TerminologyService terminologyService,
      ApplicationContext context) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.organisationLookupService = Objects.requireNonNull(organisationLookupService);
    this.measurementLookupService = Objects.requireNonNull(measurementLookupService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.measurementRepository = Objects.requireNonNull(measurementRepository);
    this.context = Objects.requireNonNull(context);
    this.terminologyService = Objects.requireNonNull(terminologyService);

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

  public long countProteomicsMeasurements(ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.countProteomicsMeasurementsBySampleIds(samplesInExperiment);
  }

  public long countNGSMeasurements(ExperimentId experimentId) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.countNGSMeasurementsBySampleIds(samplesInExperiment);
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<ProteomicsMeasurement> findProteomicsMeasurements(String filter,
      ExperimentId experimentId, int offset, int limit, List<SortOrder> sortOrder,
      ProjectId projectId) {
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

  public Optional<ProteomicsMeasurement> findProteomicsMeasurement(String measurementCode) {
    return measurementLookupService.findProteomicsMeasurement(measurementCode);
  }

  @PostAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public Collection<NGSMeasurement> findNGSMeasurements(String filter, ExperimentId experimentId,
      int offset, int limit, List<SortOrder> sortOrders, ProjectId projectId) {
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

  /**
   * Find a measurement for a given measurement code.
   *
   * @param measurementCode the measurement code (its natural ID known to the user)
   * @return an {@link Optional} of {@link NGSMeasurement}. Is {@link Optional#empty()} if no matching measurement was found.
   * @deprecated this method is unsafe, since it bypasses Spring security checks for access rights. Please use {@link #findNGSMeasurementById(String, String)} instead.
   */
  @Deprecated(since = "1.11.0", forRemoval = true)
  public Optional<NGSMeasurement> findNGSMeasurement(String measurementCode) {
    return measurementLookupService.findNGSMeasurement(measurementCode);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public Optional<NGSMeasurement> findNGSMeasurementById(String projectId, String measurementId) {
    return measurementLookupService.findNGSMeasurementById(measurementId);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public Optional<ProteomicsMeasurement> findProteomicsMeasurementById(String projectId,
      String measurementId) {
    return measurementLookupService.findProteomicsMeasurementById(measurementId);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public MeasurementRegistrationInformationNGS registerMeasurementNGS(ProjectId projectId,
      MeasurementRegistrationInformationNGS measurement) {

    // 1. Setup domain event cache and dispatcher to listen to domain events
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementCreatedDomainEventSubscriber(domainEventsCache));

    // 2. Perform actual registration
    performRegistrationNGS(measurement, projectId.value());

    // 3. Dispatch domain events
    domainEventsCache.forEach(
        domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));

    // 4. Return measurement information
    return measurement;
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public MeasurementRegistrationInformationPxP registerMeasurementPxP(ProjectId projectId,
      MeasurementRegistrationInformationPxP measurement) {
    // 1. Setup domain event cache and dispatcher to listen to domain events
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementCreatedDomainEventSubscriber(domainEventsCache));

    // 2. Perform actual registration
    performRegistrationPxP(measurement, projectId.value());

    // 3. Dispatch domain events
    domainEventsCache.forEach(
        domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));

    // 4. Return measurement information
    return measurement;
  }

  private void performUpdatePxP(MeasurementUpdateInformationPxP measurement,
      String projectId) {
    var sampleCodeEntries = buildSampleIdCodeEntries(measurement.measuredSamples());
    var measurementDomain = toDomainUpdate(measurement, sampleCodeEntries);
    measurementDomainService.updateProteomicsAll(List.of(measurementDomain));
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public MeasurementUpdateInformationNGS updateMeasurementNGS(String projectId,
      MeasurementUpdateInformationNGS measurement) throws MeasurementUpdateException {
    // 1. Setup domain event cache and dispatcher to listen to domain events
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementUpdatedDomainEventSubscriber(domainEventsCache));

    // 2. Perform actual update
    performUpdateNGS(measurement, projectId);

    // 3. Dispatch domain events
    domainEventsCache.forEach(
        domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));

    // 4. Return measurement information
    return measurement;
  }

  private void performUpdateNGS(MeasurementUpdateInformationNGS measurement, String projectId) {
    var sampleCodeEntries = buildSampleIdCodeEntries(measurement.measuredSamples());
    var measurementDomain = toDomainUpdate(measurement, sampleCodeEntries);
    measurementDomainService.updateNGSAll(List.of(measurementDomain));
  }

  @NonNull
  private NGSMeasurement toDomainUpdate(MeasurementUpdateInformationNGS measurement,
      List<SampleIdCodeEntry> sampleCodeEntries) {
    if (measurementIdMissing(measurement)) {
      throw new MeasurementUpdateException(ErrorCode.MISSING_MEASUREMENT_ID);
    }
    if (measurementUnknown(measurement)) {
      throw new MeasurementUpdateException(ErrorCode.UNKNOWN_MEASUREMENT);
    }

    var measurementDomain = measurementRepository.findNGSMeasurement(
        measurement.measurementId()).orElseThrow();
    measurementDomain.setSpecificMetadata(
        convertSpecificMetadataNGS(measurement.specificMetadata(), sampleCodeEntries));
    var organisationQuery = organisationLookupService.organisation(
        measurement.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementUpdateException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var msDeviceQuery = resolveOntologyCURI(measurement.instrumentCURIE());
    if (msDeviceQuery.isEmpty()) {
      throw new MeasurementUpdateException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new NGSMethodMetadata(msDeviceQuery.get(),
        measurement.facility(),
        measurement.sequencingReadType(),
        measurement.libraryKit(),
        measurement.flowCell(),
        measurement.sequencingRunProtocol());

    measurementDomain.setOrganisation(organisationQuery.get());
    measurementDomain.updateMethod(method);
    measurementDomain.setMeasurementName(measurement.measurementName());
    return measurementDomain;
  }

  private boolean measurementUnknown(MeasurementUpdateInformationNGS measurement) {
    return measurementRepository.findNGSMeasurement(measurement.measurementId()).isEmpty();
  }

  private boolean measurementUnknown(MeasurementUpdateInformationPxP measurement) {
    return measurementRepository.findProteomicsMeasurement(measurement.measurementId()).isEmpty();
  }

  private boolean measurementIdMissing(MeasurementUpdateInformationNGS measurement) {
    return measurementIdMissing(measurement.measurementId());
  }

  private boolean measurementIdMissing(MeasurementUpdateInformationPxP measurement) {
    return measurementIdMissing(measurement.measurementId());
  }

  private static boolean measurementIdMissing(String measurementId) {
    return measurementId.isEmpty();
  }

  @NonNull
  private ProteomicsMeasurement toDomainUpdate(MeasurementUpdateInformationPxP measurement,
      List<SampleIdCodeEntry> sampleCodeEntries) {
    if (measurementIdMissing(measurement)) {
      throw new MeasurementUpdateException(ErrorCode.MISSING_MEASUREMENT_ID);
    }

    var measurementDomain = measurementRepository.findProteomicsMeasurement(
        measurement.measurementId()).orElseThrow(() -> new MeasurementUpdateException(ErrorCode.UNKNOWN_MEASUREMENT));
    measurementDomain.setSpecificMetadata(
        convertSpecificMetadataPxP(measurement.specificMetadata(), sampleCodeEntries));
    var organisationQuery = organisationLookupService.organisation(
        measurement.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementUpdateException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var msDeviceQuery = resolveOntologyCURI(measurement.msDeviceCURIE());
    if (msDeviceQuery.isEmpty()) {
      throw new MeasurementUpdateException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new ProteomicsMethodMetadata(
        msDeviceQuery.get(),
        measurement.technicalReplicateName(),
        measurement.facility(),
        measurement.digestionMethod(),
        measurement.digestionEnzyme(),
        measurement.enrichmentMethod(),
        measurement.lcColumn(),
        measurement.lcmsMethod(),
        convertInjectionVolumeFromString(measurement.injectionVolume()),
        measurement.labelingType());

    measurementDomain.setOrganisation(organisationQuery.get());
    measurementDomain.updateMethod(method);
    measurementDomain.setMeasurementName(measurement.measurementName());
    return measurementDomain;
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public MeasurementUpdateInformationPxP updateMeasurementPxP(String projectId,
      MeasurementUpdateInformationPxP measurement) throws MeasurementUpdateException {
    // 1. Setup domain event cache and dispatcher to listen to domain events
    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementUpdatedDomainEventSubscriber(domainEventsCache));

    // 2. Perform actual update
    performUpdatePxP(measurement, projectId);

    // 3. Dispatch domain events
    domainEventsCache.forEach(
        domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));

    // 4. Return measurement information
    return measurement;
  }

  private void performRegistrationPxP(MeasurementRegistrationInformationPxP measurement,
      String projectId) {
    var sampleIdCodeEntries = buildSampleIdCodeEntries(measurement.measuredSamples());
    var measurementDomain = toDomain(measurement, projectId, sampleIdCodeEntries);
    measurementDomainService.addProteomicsAll(Map.of(measurementDomain, sampleIdCodeEntries));
  }

  private void performRegistrationNGS(MeasurementRegistrationInformationNGS measurement,
      String projectId) {
    var sampleIdCodeEntries = buildSampleIdCodeEntries(measurement.measuredSamples());
    var measurementDomain = toDomain(measurement, projectId, sampleIdCodeEntries);
    measurementDomainService.addNGSAll(Map.of(measurementDomain, sampleIdCodeEntries));
  }

  private List<SampleIdCodeEntry> buildSampleIdCodeEntries(
      List<String> sampleIds) {
    var codeEntries = new ArrayList<SampleIdCodeEntry>();
    for (String sample : sampleIds) {
      var codeEntry = sampleInformationService.findSampleId(SampleCode.create(sample));
      codeEntries.add(codeEntry.orElseThrow(
          () -> new MeasurementRegistrationException(ErrorCode.MISSING_ASSOCIATED_SAMPLE)));
    }
    return codeEntries;
  }

  private ProteomicsMeasurement toDomain(MeasurementRegistrationInformationPxP measurement,
      String projectId, List<SampleIdCodeEntry> sampleIdCodeEntries) {
    var organisationQuery = organisationLookupService.organisation(measurement.organisationId());

    if (organisationQuery.isEmpty()) {
      log.error("No organisation found for organisation id " + measurement.organisationId());
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var instrumentQuery = resolveOntologyCURI(measurement.msDeviceCURIE());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(),
        measurement.technicalReplicateName(),
        measurement.facility(),
        measurement.digestionMethod(),
        measurement.digestionEnzyme(),
        measurement.enrichmentMethod(),
        measurement.lcColumn(),
        measurement.lcmsMethod(),
        convertInjectionVolumeFromString(measurement.injectionVolume()),
        measurement.labelingType());

    var specificMetadata = convertSpecificMetadataPxP(measurement.specificMetadata(),
        sampleIdCodeEntries);

    var assignedMeasurementCode = MeasurementCode.createMS(
        sampleIdCodeEntries.getFirst().sampleCode().code());
    var assignedMeasurementName = measurement.measurementName();

    var domainMeasurement = ProteomicsMeasurement.create(ProjectId.parse(projectId),
        assignedMeasurementCode, assignedMeasurementName, organisationQuery.get(), method,
        specificMetadata);
    domainMeasurement.setSamplePoolGroup(measurement.samplePoolGroup());
    return domainMeasurement;
  }

  private static int convertInjectionVolumeFromString(String value) {
    if (value.isBlank()) {
      return 0;
    }

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      // decimal values are not supported, so we take the integer value and ignore the fraction
      return (int) Double.parseDouble(value);
    }
  }

  private NGSMeasurement toDomain(MeasurementRegistrationInformationNGS measurement,
      String projectId, List<SampleIdCodeEntry> sampleIdCodeEntries) {
    var organisationQuery = organisationLookupService.organisation(measurement.organisationId());

    if (organisationQuery.isEmpty()) {
      log.error("No organisation found for organisation id " + measurement.organisationId());
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var instrumentQuery = resolveOntologyCURI(measurement.instrumentCURIE());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), measurement.facility(),
        measurement.sequencingReadType(), measurement.libraryKit(), measurement.flowCell(),
        measurement.sequencingRunProtocol());

    var specificMetadata = convertSpecificMetadataNGS(measurement.specificMetadata(),
        sampleIdCodeEntries);

    if (measurement.samplePoolGroup().isBlank()) {
      return NGSMeasurement.createSingleMeasurement(ProjectId.parse(projectId),
          MeasurementCode.createNGS(measurement.measuredSamples().getFirst()),
          measurement.measurementName(),
          organisationQuery.get(),
          method,
          specificMetadata.getFirst());
    } else {
      return NGSMeasurement.createWithPool(
          ProjectId.parse(projectId),
          measurement.samplePoolGroup(),
          MeasurementCode.createNGS(measurement.measuredSamples().getFirst()),
          measurement.measurementName(),
          organisationQuery.get(),
          method,
          specificMetadata
      );
    }
  }

  private List<ProteomicsSpecificMeasurementMetadata> convertSpecificMetadataPxP(
      Map<String, MeasurementSpecificPxP> measurementSpecificPxPMap,
      List<SampleIdCodeEntry> sampleIdCodeEntries) {
    var specificMetadata = new ArrayList<ProteomicsSpecificMeasurementMetadata>();
    for (Map.Entry<String, MeasurementSpecificPxP> entry : measurementSpecificPxPMap.entrySet()) {
      var sampleId = entry.getKey();
      var metadata = entry.getValue();
      var convertedMetadata = ProteomicsSpecificMeasurementMetadata.create(
          sampleIdCodeEntries.stream()
              .filter(pair -> pair.sampleCode().equals(SampleCode.create(sampleId))).findAny().get()
              .sampleId(), metadata.label(), metadata.fractionName(), metadata.comment());
      specificMetadata.add(convertedMetadata);
    }
    return specificMetadata;
  }

  private List<NGSSpecificMeasurementMetadata> convertSpecificMetadataNGS(
      Map<String, MeasurementSpecificNGS> stringMeasurementSpecificNGSMap,
      List<SampleIdCodeEntry> sampleIdCodeEntries) {

    var specificMetadata = new ArrayList<NGSSpecificMeasurementMetadata>();
    for (Map.Entry<String, MeasurementSpecificNGS> entry : stringMeasurementSpecificNGSMap.entrySet()) {
      var sampleId = entry.getKey();
      var metadata = entry.getValue();
      var convertedMetadata = NGSSpecificMeasurementMetadata.create(
          sampleIdCodeEntries.stream()
              .filter(pair -> pair.sampleCode().equals(SampleCode.create(sampleId))).findAny().get()
              .sampleId(), metadata.indexI5(), metadata.indexI7(), metadata.comment());
      specificMetadata.add(convertedMetadata);
    }
    return specificMetadata;
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
   * @deprecated please use the dedicated endpoints, like
   * {@link MeasurementService#registerMeasurementPxP(ProjectId,
   * MeasurementRegistrationInformationPxP)} or
   * {@link MeasurementService#registerMeasurementNGS(ProjectId,
   * MeasurementRegistrationInformationNGS)}
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Async
  @Deprecated(since = "1.11.0", forRemoval = true)
  public CompletableFuture<List<Result<MeasurementId, ErrorCode>>> registerAll(
      List<MeasurementMetadata> measurementMetadataList, ProjectId projectId) {

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementCreatedDomainEventSubscriber(domainEventsCache));

    List<Result<MeasurementId, ErrorCode>> results;

    try {
      runPreRegistrationChecks(measurementMetadataList, projectId);
    } catch (MeasurementRegistrationException e) {
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    }
    try {
      results = context.getBean(MeasurementService.class)
          .performRegistration(measurementMetadataList, projectId).stream()
          .map(Result::<MeasurementId, ErrorCode>fromValue).toList();
    } catch (MeasurementRegistrationException e) {
      log.error("Failed to register measurement", e);
      return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
    } catch (RuntimeException e) {
      log.error("Failed to register measurement", e);
      return CompletableFuture.completedFuture(List.of(Result.fromError(ErrorCode.FAILED)));
    }
    // if the creation worked, we forward the events, otherwise it will be rolled back
    if (results.stream().allMatch(Result::isValue)) {
      domainEventsCache.forEach(
          domainEvent -> DomainEventDispatcher.instance().dispatch(domainEvent));
    }

    return CompletableFuture.completedFuture(results);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  @Transactional
  @Deprecated(since = "1.11.0", forRemoval = true)
  public List<MeasurementId> performRegistration(
      List<? extends MeasurementMetadata> measurementMetadataList, ProjectId projectId) {
    if (measurementMetadataList.isEmpty()) {
      return new ArrayList<>(); // Nothing to do
    }
    if (measurementMetadataList.get(0) instanceof ProteomicsMeasurementMetadata) {
      return performRegistrationPxP((List<MeasurementMetadata>) measurementMetadataList, projectId);

    }
    if (measurementMetadataList.get(0) instanceof NGSMeasurementMetadata) {
      return performRegistrationNGS((List<MeasurementMetadata>) measurementMetadataList, projectId);
    }
    throw new MeasurementRegistrationException(ErrorCode.FAILED);
  }

  private List<MeasurementId> performRegistrationNGS(
      List<MeasurementMetadata> measurementMetadataList,
      ProjectId projectId) {
    List<NGSMeasurementMetadata> ngsMeasurementMetadata = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof NGSMeasurementMetadata ngsMetadata) {
        ngsMeasurementMetadata.add(ngsMetadata);
      }
    }

    // Start with the pooled measurements first and group the metadata entries by pool
    Map<String, List<NGSMeasurementMetadata>> measurementsByPool = ngsMeasurementMetadata.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isPresent())
        .collect(Collectors.groupingBy(metadata -> metadata.assignedSamplePoolGroup().get()));

    // We collect the "single" sample measurements extra
    List<NGSMeasurementMetadata> singleMeasurements = ngsMeasurementMetadata.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isEmpty()).toList();

    // Then merge and prepare the domain objects by pool
    Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping = new HashMap<>(
        mergeByPoolNGS(measurementsByPool, projectId));
    // and last but not least also the single sample measurements
    singleMeasurements.stream()
        .map(singleMeasurement -> buildNGS(List.of(singleMeasurement), projectId))
        .forEach(ngsMeasurementsMapping::putAll);

    return measurementDomainService.addNGSAll(ngsMeasurementsMapping);
  }

  private List<MeasurementId> performRegistrationPxP(
      List<MeasurementMetadata> measurementMetadataList,
      ProjectId projectId) {
    List<ProteomicsMeasurementMetadata> proteomicsMeasurements = new ArrayList<>();
    for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
      if (measurementMetadata instanceof ProteomicsMeasurementMetadata proteomicsMetadata) {
        proteomicsMeasurements.add(proteomicsMetadata);
      }
    }

    // Start with the pooled measurements first and group the metadata entries by pool
    Map<String, List<ProteomicsMeasurementMetadata>> measurementsByPool = proteomicsMeasurements.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isPresent())
        .collect(Collectors.groupingBy(metadata -> metadata.assignedSamplePoolGroup().get()));

    // We collect the "single" sample measurements extra
    List<ProteomicsMeasurementMetadata> singleMeasurements = proteomicsMeasurements.stream()
        .filter(metadata -> metadata.assignedSamplePoolGroup().isEmpty()).toList();

    // Then merge and prepare the domain objects by pool
    Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping = new HashMap<>(
        mergeByPoolPxP(measurementsByPool, projectId));
    // and last but not least also the single sample measurements
    singleMeasurements.stream()
        .map(singleMeasurement -> buildPxP(List.of(singleMeasurement), projectId))
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
  private Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> mergeByPoolPxP(
      Map<String, List<ProteomicsMeasurementMetadata>> groupedMetadata, ProjectId projectId) {
    Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> metadataMap = new HashMap<>();
    for (var metadataGroup : groupedMetadata.entrySet()) {
      metadataMap.putAll(buildPxP(metadataGroup.getValue(), projectId));
    }
    return metadataMap;
  }

  private Map<NGSMeasurement, Collection<SampleIdCodeEntry>> mergeByPoolNGS(
      Map<String, List<NGSMeasurementMetadata>> groupedMetadata, ProjectId projectId) {
    Map<NGSMeasurement, Collection<SampleIdCodeEntry>> metadataMap = new HashMap<>();
    for (var metadataGroup : groupedMetadata.entrySet()) {
      metadataMap.putAll(buildNGS(metadataGroup.getValue(), projectId));
    }
    return metadataMap;
  }

  private Map<NGSMeasurement, Collection<SampleIdCodeEntry>> buildNGS(
      List<NGSMeasurementMetadata> metadataList, ProjectId projectId) {
    Map<SampleCode, SampleIdCodeEntry> sampleIdLookupTable = buildSampleIdLookupTable(metadataList);
    var sampleCodes = sampleIdLookupTable.keySet();
    var specificMetadata = createSpecificMetadataNGS(metadataList, sampleIdLookupTable);
    var assignedMeasurementCode = MeasurementCode.createNGS(sampleCodes.iterator().next().code());
    var firstMetadataEntry = metadataList.get(0);
    var assignedMeasurementName = firstMetadataEntry.measurmentName();
    var organisationQuery = organisationLookupService.organisation(
        firstMetadataEntry.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var instrumentQuery = resolveOntologyCURI(firstMetadataEntry.instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new NGSMethodMetadata(instrumentQuery.get(), firstMetadataEntry.facility(),
        firstMetadataEntry.sequencingReadType(), firstMetadataEntry.libraryKit(),
        firstMetadataEntry.flowCell(), firstMetadataEntry.sequencingRunProtocol());

    NGSMeasurement measurement;
    if (firstMetadataEntry.assignedSamplePoolGroup().isPresent()) {
      measurement = NGSMeasurement.createWithPool(projectId,
          firstMetadataEntry.assignedSamplePoolGroup().orElseThrow(), assignedMeasurementCode,
          assignedMeasurementName,
          organisationQuery.get(), method, specificMetadata);
    } else {
      measurement = NGSMeasurement.createSingleMeasurement(projectId, assignedMeasurementCode,
          assignedMeasurementName,
          organisationQuery.get(), method, specificMetadata.get(0));
    }

    return Map.of(measurement, sampleIdLookupTable.values());
  }

  private Map<SampleCode, SampleIdCodeEntry> buildSampleIdLookupTable(
      Collection<? extends MeasurementMetadata> metadata) {
    Map<SampleCode, SampleIdCodeEntry> sampleIdLookupTable = new HashMap<>();
    var sampleCodes = metadata.stream().map(MeasurementMetadata::associatedSample).toList();
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
  private Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> buildPxP(
      List<ProteomicsMeasurementMetadata> metadataList, ProjectId projectId) {
    Map<SampleCode, SampleIdCodeEntry> sampleIdLookupTable = buildSampleIdLookupTable(metadataList);
    var sampleCodes = sampleIdLookupTable.keySet();
    var specificMetadata = createSpecificMetadataPxP(metadataList, sampleIdLookupTable);
    var assignedMeasurementCode = MeasurementCode.createMS(sampleCodes.iterator().next().code());
    var firstMetadataEntry = metadataList.get(0);
    var measurementName = firstMetadataEntry.measurementName();

    var organisationQuery = organisationLookupService.organisation(
        firstMetadataEntry.organisationId());
    if (organisationQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var msDeviceQuery = resolveOntologyCURI(firstMetadataEntry.msDeviceCURIE());
    if (msDeviceQuery.isEmpty()) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var method = new ProteomicsMethodMetadata(msDeviceQuery.get(),
        firstMetadataEntry.technicalReplicateName(),
        firstMetadataEntry.facility(),
        firstMetadataEntry.digestionMethod(),
        firstMetadataEntry.digestionEnzyme(),
        firstMetadataEntry.enrichmentMethod(),
        firstMetadataEntry.lcColumn(),
        firstMetadataEntry.lcmsMethod(),
        readInjectionVolume(firstMetadataEntry.injectionVolume()),
        firstMetadataEntry.labeling()
            .labelType());

    var measurement = ProteomicsMeasurement.create(projectId, assignedMeasurementCode,
        measurementName, organisationQuery.get(), method, specificMetadata);

    measurement.setSamplePoolGroup(firstMetadataEntry.samplePoolGroup());

    return Map.of(measurement, sampleIdLookupTable.values());
  }

  private List<ProteomicsSpecificMeasurementMetadata> createSpecificMetadataPxP(
      List<ProteomicsMeasurementMetadata> metadata,
      Map<SampleCode, SampleIdCodeEntry> sampleIdCodeLookupTable) {
    return metadata.stream().map(metadataEntry -> ProteomicsSpecificMeasurementMetadata.create(
        sampleIdCodeLookupTable.get(metadataEntry.associatedSample()).sampleId(),
        metadataEntry.labeling().label(), metadataEntry.fractionName(),
        metadataEntry.comment())).toList();
  }

  private List<NGSSpecificMeasurementMetadata> createSpecificMetadataNGS(
      List<NGSMeasurementMetadata> metadata,
      Map<SampleCode, SampleIdCodeEntry> sampleIdCodeLookupTable) {
    return metadata.stream().map(metadataEntry -> NGSSpecificMeasurementMetadata.create(
        sampleIdCodeLookupTable.get(metadataEntry.associatedSample()).sampleId(),
        metadataEntry.indexI5(), metadataEntry.indexI7(),
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

    List<DomainEvent> domainEventsCache = new ArrayList<>();
    var localDomainEventDispatcher = LocalDomainEventDispatcher.instance();
    localDomainEventDispatcher.reset();
    localDomainEventDispatcher.subscribe(
        new MeasurementUpdatedDomainEventSubscriber(domainEventsCache));

    if (!measurementMetadataList.isEmpty() && measurementMetadataList.get(
        0) instanceof ProteomicsMeasurementMetadata) {
      List<ProteomicsMeasurementMetadata> metadata = new ArrayList<>();
      for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
        metadata.add((ProteomicsMeasurementMetadata) measurementMetadata);
      }
      try {
        results = updateAllPxP(metadata, projectId);
      } catch (MeasurementRegistrationException e) {
        log.error("Measurement update failed.", e);
        return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
      }
      handleUpdateEvents(domainEventsCache, results);
      return CompletableFuture.completedFuture(results);
    }

    if (!measurementMetadataList.isEmpty() && measurementMetadataList.get(
        0) instanceof NGSMeasurementMetadata) {
      List<NGSMeasurementMetadata> metadata = new ArrayList<>();
      for (MeasurementMetadata measurementMetadata : measurementMetadataList) {
        metadata.add((NGSMeasurementMetadata) measurementMetadata);
      }
      try {
        results = updateAllNGS(metadata, projectId);
      } catch (MeasurementRegistrationException e) {
        log.error("Measurement update failed.", e);
        return CompletableFuture.completedFuture(List.of(Result.fromError(e.reason)));
      }
      handleUpdateEvents(domainEventsCache, results);
      return CompletableFuture.completedFuture(results);
    }
    return CompletableFuture.completedFuture(List.of(Result.fromError(ErrorCode.FAILED)));
  }

  private void handleUpdateEvents(List<DomainEvent> domainEventsCache,
      List<Result<MeasurementId, ErrorCode>> results) {
    if (results.stream().anyMatch(Result::isError)) {
      return;
    }
    Set<MeasurementId> dispatchedIDs = new HashSet<>();
    for (DomainEvent event : domainEventsCache) {
      if (event instanceof MeasurementUpdatedEvent measurementUpdatedEvent) {
        MeasurementId id = measurementUpdatedEvent.measurementId();
        if (dispatchedIDs.contains(id)) {
          continue;
        }
        DomainEventDispatcher.instance().dispatch(event);
        dispatchedIDs.add(id);
      }
    }

  }

  /**
   * Reads the injection volume from a character representation.
   * <p>
   * If no value is present, it returns -1.
   *
   * @param value
   * @return
   * @since 1.0.0
   */
  private int readInjectionVolume(String value) throws NumberFormatException {
    if (value.isBlank()) {
      return -1;
    }
    return (int) Double.parseDouble(value);
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
  private List<Result<MeasurementId, ErrorCode>> updateAllPxP(
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
          createSpecificMetadataPxP(List.of(measurementMetadata), lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          measurementMetadata.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var msDeviceQuery = resolveOntologyCURI(measurementMetadata.msDeviceCURIE());
      if (msDeviceQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new ProteomicsMethodMetadata(msDeviceQuery.get(),
          measurementMetadata.technicalReplicateName(),
          measurementMetadata.facility(),
          measurementMetadata.digestionMethod(), measurementMetadata.digestionEnzyme(),
          measurementMetadata.enrichmentMethod(), measurementMetadata.lcColumn(),
          measurementMetadata.lcmsMethod(),
          readInjectionVolume(measurementMetadata.injectionVolume()),
          measurementMetadata.labeling()
              .labelType());

      measurement.setOrganisation(organisationQuery.get());
      measurement.updateMethod(method);
      measurementsForUpdate.add(measurement);
    }

    for (String measurementId : pooledMeasurements.keySet()) {
      var pooledMeasurement = pooledMeasurements.get(measurementId);
      var firstEntry = pooledMeasurement.get(0);
      var measurement = measurementRepository.findProteomicsMeasurement(
          firstEntry.measurementId()).orElseThrow();
      measurement.setSpecificMetadata(createSpecificMetadataPxP(pooledMeasurement, lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          firstEntry.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var msDeviceQuery = resolveOntologyCURI(firstEntry.msDeviceCURIE());
      if (msDeviceQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new ProteomicsMethodMetadata(msDeviceQuery.get(),
          firstEntry.technicalReplicateName(),
          firstEntry.facility(),
          firstEntry.digestionMethod(),
          firstEntry.digestionEnzyme(),
          firstEntry.enrichmentMethod(),
          firstEntry.lcColumn(),
          firstEntry.lcmsMethod(),
          readInjectionVolume(firstEntry.injectionVolume()),
          firstEntry.labeling()
              .labelType());

      measurement.setOrganisation(organisationQuery.get());
      measurement.updateMethod(method);
      measurementsForUpdate.add(measurement);
    }

    try {
      var ids = measurementDomainService.updateProteomicsAll(measurementsForUpdate);
      return ids.stream().map(Result::<MeasurementId, ErrorCode>fromValue).toList();
    } catch (RuntimeException e) {
      return List.of(Result.fromError(ErrorCode.FAILED));
    }
  }


  private List<Result<MeasurementId, ErrorCode>> updateAllNGS(
      List<NGSMeasurementMetadata> metadata, ProjectId projectId) {

    if (measurementCodeMissing(metadata)) {
      throw new MeasurementRegistrationException(ErrorCode.MISSING_MEASUREMENT_ID);
    }
    if (!allMeasurementCodesExist(
        metadata.stream().map(NGSMeasurementMetadata::measurementId).toList())) {
      throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_MEASUREMENT);
    }

    var singleSampleMeasurements = metadata.stream()
        .filter(measurement -> measurement.assignedSamplePoolGroup().isEmpty()).toList();
    var pooledMeasurements = metadata.stream()
        .filter(measurement -> measurement.assignedSamplePoolGroup().isPresent()).collect(
            Collectors.groupingBy(NGSMeasurementMetadata::measurementId));

    List<NGSMeasurement> measurementsForUpdate = new ArrayList<>();

    var lookupTable = buildSampleIdLookupTable(metadata);

    for (NGSMeasurementMetadata measurementMetadata : singleSampleMeasurements) {
      var measurement = measurementRepository.findNGSMeasurement(
          measurementMetadata.measurementId()).orElseThrow();
      measurement.setSpecificMetadata(
          createSpecificMetadataNGS(List.of(measurementMetadata), lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          measurementMetadata.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var instrumentQuery = resolveOntologyCURI(measurementMetadata.instrumentCURI());
      if (instrumentQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new NGSMethodMetadata(instrumentQuery.get(),
          measurementMetadata.facility(), measurementMetadata.sequencingReadType(),
          measurementMetadata.libraryKit(), measurementMetadata.flowCell(),
          measurementMetadata.sequencingRunProtocol());

      measurement.setOrganisation(organisationQuery.get());
      measurement.updateMethod(method);
      measurementsForUpdate.add(measurement);
    }

    for (String measurementId : pooledMeasurements.keySet()) {
      var pooledMeasurement = pooledMeasurements.get(measurementId);
      var firstEntry = pooledMeasurement.get(0);
      var measurement = measurementRepository.findNGSMeasurement(
          firstEntry.measurementId()).orElseThrow();
      measurement.setSpecificMetadata(createSpecificMetadataNGS(pooledMeasurement, lookupTable));
      var organisationQuery = organisationLookupService.organisation(
          firstEntry.organisationId());
      if (organisationQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ORGANISATION_ROR_ID);
      }

      var instrumentQuery = resolveOntologyCURI(firstEntry.instrumentCURI());
      if (instrumentQuery.isEmpty()) {
        throw new MeasurementRegistrationException(ErrorCode.UNKNOWN_ONTOLOGY_TERM);
      }

      var method = new NGSMethodMetadata(instrumentQuery.get(),
          firstEntry.facility(), firstEntry.sequencingReadType(),
          firstEntry.libraryKit(), firstEntry.flowCell(),
          firstEntry.sequencingRunProtocol());

      measurement.setOrganisation(organisationQuery.get());
      measurement.updateMethod(method);
      measurementsForUpdate.add(measurement);
    }

    try {
      var ids = measurementDomainService.updateNGSAll(measurementsForUpdate);
      return ids.stream().map(Result::<MeasurementId, ErrorCode>fromValue).toList();
    } catch (RuntimeException e) {
      return List.of(Result.fromError(ErrorCode.FAILED));
    }
  }

  private boolean measurementCodeMissing(List<? extends MeasurementMetadata> metadata) {
    return metadata.stream().map(MeasurementMetadata::measurementIdentifier)
        .anyMatch(Optional::isEmpty);
  }

  private boolean allMeasurementCodesExist(List<String> measurementCode) {
    return measurementCode.stream()
        .allMatch(measurementRepository::existsMeasurement);
  }

  private Optional<OntologyTerm> resolveOntologyCURI(String ontologyCURI) {
    return terminologyService.findByCurie(ontologyCURI);
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
  public Result<Void, MeasurementDeletionException> deletePxPMeasurements(ProjectId projectId,
      Set<ProteomicsMeasurement> selectedMeasurements) {
    try {
      measurementDomainService.deletePxP(selectedMeasurements);
      if (!selectedMeasurements.isEmpty()) {
        dispatchProjectChangedOnMeasurementDeleted(projectId);
      }
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
      if (!selectedMeasurements.isEmpty()) {
        dispatchProjectChangedOnMeasurementDeleted(projectId);
      }
      return Result.fromValue(null);
    } catch (MeasurementDeletionException e) {
      return Result.fromError(e);
    }
  }

  private void dispatchProjectChangedOnMeasurementDeleted(ProjectId projectId) {
    ProjectChanged projectChanged = ProjectChanged.create(projectId);
    DomainEventDispatcher.instance().dispatch(projectChanged);
  }

  public enum DeletionErrorCode {
    FAILED, DATA_ATTACHED
  }

  public enum ErrorCode {
    FAILED, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, WRONG_EXPERIMENT,
    MISSING_ASSOCIATED_SAMPLE, MISSING_MEASUREMENT_ID, SAMPLECODE_NOT_FROM_PROJECT,
    UNKNOWN_MEASUREMENT
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

  public static final class MeasurementUpdateException extends RuntimeException {

    private final ErrorCode reason;

    public MeasurementUpdateException(ErrorCode reason) {
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

  private record MeasurementCreatedDomainEventSubscriber(
      List<DomainEvent> domainEventsCache) implements
      DomainEventSubscriber<DomainEvent> {

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
      return MeasurementCreatedEvent.class;
    }

    @Override
    public void handleEvent(DomainEvent event) {
      domainEventsCache.add(event);
    }
  }
}
