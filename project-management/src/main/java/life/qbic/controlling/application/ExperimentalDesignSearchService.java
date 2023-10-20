package life.qbic.controlling.application;

import java.util.List;
import life.qbic.controlling.domain.model.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.controlling.domain.model.experiment.vocabulary.Analyte;
import life.qbic.controlling.domain.model.experiment.vocabulary.Species;
import life.qbic.controlling.domain.model.experiment.vocabulary.Specimen;
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

  public List<Species> retrieveSpecies() {
    return experimentalDesignVocabularyRepository.retrieveSpecies();
  }

  public List<Specimen> retrieveSpecimens() {
    return experimentalDesignVocabularyRepository.retrieveSpecimens();
  }

  public List<Analyte> retrieveAnalytes() {
    return experimentalDesignVocabularyRepository.retrieveAnalytes();
  }

}
