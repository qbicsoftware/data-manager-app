package life.qbic.projectmanagement.domain.project.experiment.repository;

import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Organism;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import java.util.List;

/**
 * Queries available vocabulary terms for organisms, specimens (tissues, cells, etc.) and the analytes of a project
 *
 * @since 1.0.0
 */
public interface ExperimentalDesignVocabularyRepository {

  public List<Organism> retrieveOrganisms();

  public List<Specimen> retrieveSpecimens();

  public List<Analyte> retrieveAnalytes();

}
