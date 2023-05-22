package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.project.service.SampleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sample Registration Service
 * <p>
 * Application service allowing for retrieving the information necessary for sample registration
 */
@Service
public class SampleRegistrationService {

  private final SampleRepository sampleRepository;

  private final SampleCodeService sampleCodeService;

  private final SampleDomainService sampleDomainService;

  @Autowired
  public SampleRegistrationService(SampleRepository sampleRepository,
      SampleCodeService sampleCodeService, SampleDomainService sampleDomainService) {
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.sampleDomainService = Objects.requireNonNull(sampleDomainService);
  }

  public List<SamplesheetHeaderName> retrieveProteomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL, SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID,
        SamplesheetHeaderName.CONDITION, SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveLigandomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL, SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID,
        SamplesheetHeaderName.CONDITION, SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveMetabolomics() {
    return List.of(SamplesheetHeaderName.SAMPLE_LABEL, SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID,
        SamplesheetHeaderName.CONDITION, SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN,
        SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public List<SamplesheetHeaderName> retrieveGenomics() {
    return List.of(SamplesheetHeaderName.SEQ_ANALYSIS_TYPE, SamplesheetHeaderName.SAMPLE_LABEL,
        SamplesheetHeaderName.BIOLOGICAL_REPLICATE_ID, SamplesheetHeaderName.CONDITION,
        SamplesheetHeaderName.SPECIES, SamplesheetHeaderName.SPECIMEN, SamplesheetHeaderName.CUSTOMER_COMMENT);
  }

  public Result<Sample, ResponseCode> registerSample(
      SampleRegistrationRequest sampleRegistrationRequest, ProjectId projectId) {
    var result = sampleCodeService.generateFor(projectId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
    }
    var registration = sampleDomainService.registerSample(result.getValue(), sampleRegistrationRequest);
    if (registration.isValue()) {
      return Result.fromValue(registration.getValue());
    }
    return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
  }

  public enum ResponseCode {
    SAMPLE_REGISTRATION_FAILED
  }


  /**
   * SampleSheetHeaderLabel enums are used to refer to the labels of different columns of the spreadsheet for different technologies
   *
   * @since 1.0.0
   */
  public enum SamplesheetHeaderName {
    SEQ_ANALYSIS_TYPE("Analysis to be performed"),
    SAMPLE_LABEL("Sample label"),
    BIOLOGICAL_REPLICATE_ID("Biological replicate id"),
    CONDITION("Condition"),
    SPECIES("Species"),
    SPECIMEN("Specimen"),
    CUSTOMER_COMMENT("Customer comment");
    public final String label;

    SamplesheetHeaderName(String label) {
      this.label = label;
    }
  }

}
