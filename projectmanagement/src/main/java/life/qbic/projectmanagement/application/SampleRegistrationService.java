package life.qbic.projectmanagement.application;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Sample Registration Service
 * <p>
 * Application service allowing for retrieving the information necessary for sample registration
 */
@Service
public class SampleRegistrationService {

  public SampleRegistrationService() {
  }

  public List<String> retrieveProteomics() {
    return List.of("Sample label", "Biological replicate id", "Condition", "Species",
        "Specimen", "Customer comment");
  }

  public List<String> retrieveLigandomics() {
    return List.of("Sample label", "Biological replicate id", "Condition", "Species",
        "Specimen", "Customer comment");
  }

  public List<String> retrieveMetabolomics() {
    return List.of("Sample label", "Biological replicate id", "Condition", "Species",
        "Specimen", "Customer comment");
  }

  public List<String> retrieveGenomics() {
    return List.of("Analysis to be performed", "Sample label", "Biological replicate id",
        "Condition", "Species", "Specimen", "Customer comment");
  }

}
