package life.qbic.projectmanagement.domain.project.repository;

import java.util.List;
import life.qbic.projectmanagement.domain.project.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.vocabulary.Species;

/**
 * Queries available vocabulary terms for organisms, specimens (tissues, cells, etc.) and the analytes of a project
 *
 * @since 1.0.0
 */
public interface ExperimentalDesignVocabularyRepository {

  public List<Species> retrieveOrganisms();

  public List<Specimen> retrieveSpecimens();

  public List<Analyte> retrieveAnalytes();

}
