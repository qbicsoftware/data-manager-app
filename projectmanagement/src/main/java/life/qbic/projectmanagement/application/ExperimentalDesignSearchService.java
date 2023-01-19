package life.qbic.projectmanagement.application;

import java.util.List;
import life.qbic.projectmanagement.application.api.ExperimentalDesignLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Experimental Design Service
 * <p>
 * Application service allowing for retrieving the experimental design information
 */
@Service
public class ExperimentalDesignSearchService {

  private final ExperimentalDesignLookup experimentalDesignLookup;

  public ExperimentalDesignSearchService(
      @Autowired ExperimentalDesignLookup experimentalDesignLookup) {
    this.experimentalDesignLookup = experimentalDesignLookup;
  }

  public List<String> retrieveOrganisms() {
    return experimentalDesignLookup.retrieveOrganisms();
  }

  public List<String> retrieveSpecimens() {
    return experimentalDesignLookup.retrieveSpecimens();
  }

  public List<String> retrieveAnalytes() {
    return experimentalDesignLookup.retrieveAnalytes();
  }

}
