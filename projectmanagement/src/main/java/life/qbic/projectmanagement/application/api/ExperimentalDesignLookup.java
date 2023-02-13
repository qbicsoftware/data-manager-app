package life.qbic.projectmanagement.application.api;

import java.util.List;

public interface ExperimentalDesignLookup {

  /**
   * Retrieves a list of all species from experimental design repository
   *
   * @return list of all species
   */
  List<String> retrieveOrganisms();

  /**
   * Retrieves a list of all specimens from the experimental design repository
   *
   * @return list of all specimen
   */
  List<String> retrieveSpecimens();

  /**
   * Retrieves a list of all analytes stored in the experimental design repository
   *
   * @return list of all analytes
   */
  List<String> retrieveAnalytes();

}
