package life.qbic.projectmanagement.domain.model.experiment.repository;

import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

/**
 * Queries available vocabulary terms for species, specimens (tissues, cells, etc.) and the analytes
 * of a project
 *
 * @since 1.0.0
 */
public interface ExperimentalDesignVocabularyRepository {

  List<Specimen> retrieveSpecimens();

  List<Analyte> retrieveAnalytes();

  List<Species> retrieveSpecies();
}
