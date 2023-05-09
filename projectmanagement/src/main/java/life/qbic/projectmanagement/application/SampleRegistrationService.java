package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.SampleInformationService.Sample;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
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

  @Autowired
  public SampleRegistrationService(SampleRepository sampleRepository,
      SampleCodeService sampleCodeService) {
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
  }

  public List<String> retrieveProteomics() {
    return List.of("Sample Name", "Biological Replicate", "Treatment", "Cell Line", "Species",
        "Specimen", "Analyte", "Comment");
  }

  public List<String> retrieveLigandomics() {
    return List.of("Sample Name", "Biological Replicate", "Treatment", "Cell Line", "Species",
        "Specimen", "Analyte", "Comment");
  }

  public List<String> retrieveMetabolomics() {
    return List.of("Sample Name", "Biological Replicate", "Treatment", "Cell Line", "Species",
        "Specimen", "Analyte", "Comment");
  }

  public List<String> retrieveGenomics() {
    return List.of("Analysis to be performed", "Plate position (e.g. A1)", "Plate title",
        "Sample label", "Biological replicate reference id", "Species", "Specimen",
        "FFPE material");
  }

  public Result<Sample, ResponseCode> registerSample(
      SampleRegistrationRequest sampleRegistrationRequest, ProjectId projectId) {
    return null;
  }

  private SampleCode requestSampleCode(ProjectId projectId) {
    return null;
  }


  public enum ResponseCode {

  }

}
