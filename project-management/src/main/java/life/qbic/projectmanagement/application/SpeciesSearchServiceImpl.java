package life.qbic.projectmanagement.application;

import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Experimental Design Service
 * <p>
 * Application service allowing for retrieving the experimental design information
 */
@Service
public class SpeciesSearchServiceImpl implements SpeciesSearchService {

  private final ExperimentalDesignVocabularyRepository experimentalDesignVocabularyRepository;

  public SpeciesSearchServiceImpl(
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

  @Override
  public List<Species> fetch(int offset, int limit, String filterText) {
    return null;
  }

  @Override
  public int getCount(String filterText) {
    return 0;
  }
}
