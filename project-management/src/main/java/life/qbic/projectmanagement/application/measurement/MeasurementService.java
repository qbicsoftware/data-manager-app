package life.qbic.projectmanagement.application.measurement;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.service.MeasurementDomainService;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class MeasurementService {

  private static final Logger log = logger(MeasurementService.class);
  private final MeasurementDomainService measurementDomainService;
  private final SampleInformationService sampleInformationService;

  public MeasurementService(MeasurementDomainService measurementDomainService,
      SampleInformationService sampleInformationService) {
    this.measurementDomainService = Objects.requireNonNull(measurementDomainService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  public Result<MeasurementId, ResponseCode> createNGS(List<SampleCode> sampleCodes,
      OntologyTerm instrument) {
    var code = MeasurementCode.createNGS(sampleCodes.get(0).code());
    Collection<SampleIdCodeEntry> sampleIdCodeEntries = queryIdCodePairs(sampleCodes);
    if (sampleIdCodeEntries.size() != sampleCodes.size()) {
      log.error("Could not find all corresponding sample ids for input: " + sampleCodes);
      return Result.fromError(ResponseCode.FAILED);
    }
    var measurement = NGSMeasurement.create(
        sampleIdCodeEntries.stream().map(SampleIdCodeEntry::sampleId).map(SampleId::value).toList(),
        code,
        instrument);
    var result = measurementDomainService.addNGS(measurement);
    if (result.isError()) {
      return Result.fromError(ResponseCode.FAILED);
    } else {
      return Result.fromValue(result.getValue().measurementId().get());
    }

  }

  private Collection<SampleIdCodeEntry> queryIdCodePairs(Collection<SampleCode> sampleCodes) {
    return sampleCodes.stream().map(sampleInformationService::findSampleId)
        .filter(Optional::isPresent)
        .map(Optional::get).toList();
  }

  public enum ResponseCode {
    FAILED, SUCCESSFUL
  }

}
