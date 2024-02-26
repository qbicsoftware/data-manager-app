package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.OrganisationLookupService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.Organisation;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMethodMetadata;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class MeasurementService {

  private static final Logger log = logger(MeasurementService.class);
  private final MeasurementDomainService measurementDomainService;
  private final SampleInformationService sampleInformationService;
  private final OntologyLookupService ontologyLookupService;
  private final OrganisationLookupService organisationLookupService;

  @Autowired
  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService,
      OntologyLookupService ontologyLookupService,
      OrganisationLookupService organisationLookupService) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.ontologyLookupService = Objects.requireNonNull(ontologyLookupService);
    this.organisationLookupService = Objects.requireNonNull(organisationLookupService);
  }


  public Collection<NGSMeasurement> findNGSMeasurements(ExperimentId experimentId, int offset,
      int limit,
      List<SortOrder> sortOrders, String filter) {
    //return new ArrayList<>();
    //ToDo implement Lazy Loading in Backend
    return List.of(
        NGSMeasurement.create(List.of(SampleId.create(), SampleId.create(), SampleId.create()),
            MeasurementCode.createNGS("ABCDE"),
            new

                OntologyTerm("ontA1", "ontV1", "ontI1", "ontL1", "ontN1", "ontD1", "ontCI1")),
        NGSMeasurement.create(List.of(SampleId.create(), SampleId.create()),
            MeasurementCode.createNGS("FGHIJ"),
            new

                OntologyTerm("ontA2", "ontV2", "ontI2", "ontL2", "ontN2", "ontD2", "ontCI2")));
  }


  public Collection<ProteomicsMeasurement> findProteomicsMeasurement(ExperimentId experimentId,
      int offset, int limit,
      List<SortOrder> sortOrders, String filter) {
    //ToDo implement Lazy Loading in Backend
    //return new ArrayList<>();
    return List.of(
        ProteomicsMeasurement.create(
            List.of(SampleId.create(), SampleId.create(), SampleId.create()),
            MeasurementCode.createMS("ABCDE"), new Organisation("ProtIri1", "ProtOrglabel1"),
            new ProteomicsMethodMetadata(
                new OntologyTerm("ontA1", "ontV1", "ontI1", "ontL1", "ontN1", "ontD1", "ontCI1"),
                "ProtPSL1", "ProtFN1", "ProtFT1",
                "ProtDM1", "ProtDE1", "ProtEM1", 1, "ProtIC1", "ProtLM1")
        ), ProteomicsMeasurement.create(
            List.of(SampleId.create(), SampleId.create(), SampleId.create()),
            MeasurementCode.createMS("FGHIJ"), new Organisation("ProtIri2", "ProtOrgLabel2"),
            new ProteomicsMethodMetadata(
                new OntologyTerm("ontA2", "ontV2", "ontI2", "ontL2", "ontN2", "ontD2", "ontCI2"),
                "ProtPSL2", "ProtFN2", "ProtFT2",
                "ProtDM2", "ProtDE2", "ProtEM2", 2, "ProtIC2", "ProtLM2")
        ));
  }


  public Result<MeasurementId, ResponseCode> registerNGS(
      MeasurementRegistrationRequest<NGSMeasurementMetadata> registrationRequest) {

    var associatedSampleCodes = registrationRequest.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(registrationRequest.associatedSamples().get(0)));
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

    var result = measurementDomainService.addNGS(new NGSMeasurementWrapper(measurement, parentCodes));

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
  }

  public Result<MeasurementId, ResponseCode> registerPxP(
      MeasurementRegistrationRequest<ProteomicsMeasurementMetadata> registrationRequest) {
    var associatedSampleCodes = registrationRequest.associatedSamples();
    var selectedSampleCode = MeasurementCode.createNGS(
        String.valueOf(registrationRequest.associatedSamples().get(0)));
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

    var method = new ProteomicsMethodMetadata(instrumentQuery.get(), "", "", "", "", "", "", 0, "",
        "");

    var measurement = ProteomicsMeasurement.create(
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).toList(),
        selectedSampleCode,
        organisationQuery.get(),
        method);

    var parentCodes = sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleCode).toList();

    var result = measurementDomainService.addProteomics(new ProteomicsMeasurementWrapper(measurement, parentCodes));

    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId());
    }
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
    FAILED, SUCCESSFUL, UNKNOWN_ORGANISATION_ROR_ID, UNKNOWN_ONTOLOGY_TERM
  }

  public record ProteomicsMeasurementWrapper(ProteomicsMeasurement measurementMetadata,
                                             Collection<SampleCode> measuredSamplesCodes){};
  public record NGSMeasurementWrapper(NGSMeasurement measurementMetadata,
                                      Collection<SampleCode> measuredSamplesCodes){};

}
