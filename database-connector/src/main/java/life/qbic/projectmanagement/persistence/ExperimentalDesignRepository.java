package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.application.api.ExperimentalDesignLookup;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Simple mock implementation of the {@link ExperimentalDesignLookup} interface.
 */
@Component
public class ExperimentalDesignRepository implements ExperimentalDesignLookup {

  @Override
  public List<String> retrieveSpecies() {
    return Arrays.asList("Species1", "Species2", "Species3",
        "Species4", "Species5");
  }

  @Override
  public List<String> retrieveSpecimens() {
    return Arrays.asList("Specimen1", "Specimen2", "Specimen3",
        "Specimen4", "Specimen5");
  }

  @Override
  public List<String> retrieveAnalytes() {
    return Arrays.asList("Analyte1", "Analyte2", "Analyte3", "Analyte4",
        "Analyte5");
  }
}
