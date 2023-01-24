package life.qbic.projectmanagement.application;

import java.util.List;
import life.qbic.projectmanagement.domain.project.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.vocabulary.Organism;
import life.qbic.projectmanagement.domain.project.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Experimental Design Service
 * <p>
 * Application service allowing for retrieving the experimental design information
 */
@Service
public class ExperimentalDesignSearchService {

  private final ExperimentalDesignVocabularyRepository experimentalDesignVocabularyRepository;

  public ExperimentalDesignSearchService(
      @Autowired ExperimentalDesignVocabularyRepository experimentalDesignVocabularyRepository) {
    this.experimentalDesignVocabularyRepository = experimentalDesignVocabularyRepository;
  }

  public List<Organism> retrieveOrganisms() {
    return experimentalDesignVocabularyRepository.retrieveOrganisms();
  }

  public List<Specimen> retrieveSpecimens() {
    return experimentalDesignVocabularyRepository.retrieveSpecimens();
  }

  public List<Analyte> retrieveAnalytes() {
    return experimentalDesignVocabularyRepository.retrieveAnalytes();
  }

}
