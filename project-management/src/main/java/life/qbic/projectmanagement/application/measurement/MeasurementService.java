package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMethodMetadata;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;
import org.springframework.beans.factory.annotation.Autowired;
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


  public Collection<NGSMeasurement> findNGSMeasurements(String filter, ExperimentId experimentId,
      int offset,
      int limit,
      List<SortOrder> sortOrders) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryNGSMeasurementsBySampleIds(filter, samplesInExperiment,
        offset, limit, sortOrders);
  }


  public Collection<ProteomicsMeasurement> findProteomicsMeasurement(String filter,
      ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrder) {
    var result = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    var samplesInExperiment = result.getValue().stream().map(Sample::sampleId).toList();
    return measurementLookupService.queryProteomicsMeasurementsBySampleIds(filter,
        samplesInExperiment, offset, limit, sortOrder);
  }


  private Result<MeasurementId, ResponseCode> registerNGS(
      MeasurementRegistrationRequest<NGSMeasurementMetadata> registrationRequest) {

    var associatedSampleCodes = registrationRequest.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(registrationRequest.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      return Result.fromError(ResponseCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(registrationRequest.metadata().instrumentCURIE());
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
      MeasurementRegistrationRequest<ProteomicsMeasurementMetadata> registrationRequest) {
    var associatedSampleCodes = registrationRequest.associatedSamples();
    var selectedSampleCode = MeasurementCode.createMS(
        String.valueOf(registrationRequest.associatedSamples().get(0).code()));
    var sampleIdCodeEntries = queryIdCodePairs(associatedSampleCodes);

    if (sampleIdCodeEntries.size() != associatedSampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + associatedSampleCodes);
      return Result.fromError(ResponseCode.FAILED);
    }

    var instrumentQuery = resolveOntologyCURI(registrationRequest.metadata().instrumentCURI());
    if (instrumentQuery.isEmpty()) {
      return Result.fromError(ResponseCode.UNKNOWN_ONTOLOGY_TERM);
    }

    var organisationQuery = organisationLookupService.organisation(
        registrationRequest.metadata().organisationId());
    if (organisationQuery.isEmpty()) {
      return Result.fromError(ResponseCode.UNKNOWN_ORGANISATION_ROR_ID);
    }

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), "", "", "", "", 0, "", "");

    var measurement = ProteomicsMeasurement.create(
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        organisationQuery.get(),
        method);

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addProteomics(measurement, parentCodes);

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  public Result<MeasurementId, ResponseCode> register(
      MeasurementRegistrationRequest<? extends MeasurementMetadata> registrationRequest) {

    var associatedSampleCodes = registrationRequest.associatedSamples();
    boolean allSamplesAreOfExperiment = sampleInformationService.retrieveSamplesForExperiment(
            registrationRequest.experimentId())
        .map(samples -> (samples.stream().map(Sample::sampleCode).collect(
            Collectors.toSet())))
        .map(sampleCodes -> sampleCodes.containsAll(associatedSampleCodes))
        .fold(value -> value, error -> false);
    if (!allSamplesAreOfExperiment) {
      return Result.fromError(ResponseCode.WRONG_EXPERIMENT);
    }

    if (registrationRequest.metadata() instanceof ProteomicsMeasurementMetadata proteomicsMeasurementMetadata) {
      return registerPxP(
          new MeasurementRegistrationRequest<>(proteomicsMeasurementMetadata,
              registrationRequest.experimentId()));
    }
    if (registrationRequest.metadata() instanceof NGSMeasurementMetadata ngsMeasurementMetadata) {
      return registerNGS(
          new MeasurementRegistrationRequest<>(ngsMeasurementMetadata,
              registrationRequest.experimentId()));
    }
    return Result.fromError(ResponseCode.FAILED);
  }


  @Transactional
  public void registerMultiple(
      List<MeasurementRegistrationRequest<? extends MeasurementMetadata>> requests) {
    var mergedRequests = mergeBySamplePoolGroup(requests);
    for (MeasurementRegistrationRequest<? extends MeasurementMetadata> request : requests) {
      register(request)
          .onError(error -> {
            throw new MeasurementRegistrationException(error);
          });

    }
  }

  private List<MeasurementRegistrationRequest<? extends MeasurementMetadata>> mergeBySamplePoolGroup(
      List<MeasurementRegistrationRequest<? extends MeasurementMetadata>> requests) {
    if (!requests.isEmpty() && requests.get(0)
        .metadata() instanceof ProteomicsMeasurementMetadata) {
      return mergeBySamplePoolGroupProteomics(requests.stream().map(
              request -> new MeasurementRegistrationRequest<>(
                  (ProteomicsMeasurementMetadata) request.metadata(), request.experimentId()))
          .toList());
    }
    return requests;
  }

  private List<MeasurementRegistrationRequest<? extends MeasurementMetadata>> mergeBySamplePoolGroupProteomics(
      List<MeasurementRegistrationRequest<ProteomicsMeasurementMetadata>> requests) {
    var result = requests.stream().map(MeasurementRegistrationRequest::metadata).filter(
        proteomicsMeasurementMetadata -> proteomicsMeasurementMetadata.assignedSamplePoolGroup()
            .isPresent()).collect(Collectors.groupingBy(
        metadata -> metadata.assignedSamplePoolGroup().get()));
    // TODO continue implementation
    return null;
  }

  private static ProteomicsMeasurementMetadata merge(
      List<ProteomicsMeasurementMetadata> measurementMetadataList) {
    List<SampleCode> associatedSamples = measurementMetadataList.stream().map(
        ProteomicsMeasurementMetadata::sampleCodes).flatMap(Collection::stream).toList();
    var firstEntry = measurementMetadataList.get(0);
    return new ProteomicsMeasurementMetadata(associatedSamples, firstEntry.organisationId(), firstEntry.instrumentCURI(),
        firstEntry.samplePoolGroup());
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
    FAILED, SUCCESSFUL, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM, WRONG_EXPERIMENT
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

