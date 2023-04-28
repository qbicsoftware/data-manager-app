package life.qbic.projectmanagement.domain.project.experiment.repository;

import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import java.util.List;

/**
 * Queries available vocabulary terms for species, specimens (tissues, cells, etc.) and the
 * analytes of a project
 *
 * @since 1.0.0
 */
public interface ExperimentalDesignVocabularyRepository {

  List<Specimen> retrieveSpecimens();

  List<Analyte> retrieveAnalytes();

  List<Species> retrieveSpecies();
}
