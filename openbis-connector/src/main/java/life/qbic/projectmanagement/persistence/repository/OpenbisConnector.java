package life.qbic.projectmanagement.persistence.repository;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Organism;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements ExperimentalDesignVocabularyRepository {

  private final OpenBisClient openBisClient;

  private OpenbisConnector(@Value("${openbis.user.name}") String userName,
                           @Value("${openbis.user.password}") String password,
                           @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(
            userName, password, url);
    openBisClient.login();
  }

  private List<VocabularyTerm> getVocabularyTermsForCode(VocabularyCode vocabularyCode) {
    VocabularyTermSearchCriteria criteria = new VocabularyTermSearchCriteria();
    criteria.withVocabulary().withCode().thatEquals(vocabularyCode.openbisCode());

    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();
    SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm> searchResult =
            openBisClient.getV3().searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);

    return searchResult.getObjects().stream()
            .map(it -> new VocabularyTerm(it.getCode(), it.getLabel(), it.getDescription()))
            .toList();
  }

  @Override
  public List<Organism> retrieveOrganisms() {
    return getVocabularyTermsForCode(VocabularyCode.ORGANISM).stream()
            .map(it -> it.label().isBlank() ? it.code() : it.label())
            .map(Organism::new).toList();
  }

  @Override
  public List<Specimen> retrieveSpecimens() {
    return getVocabularyTermsForCode(VocabularyCode.SPECIMEN).stream()
            .map(it -> it.label().isBlank() ? it.code() : it.label())
            .map(Specimen::new).toList();
  }

  @Override
  public List<Analyte> retrieveAnalytes() {
    return getVocabularyTermsForCode(VocabularyCode.ANALYTE).stream()
            .map(it -> it.label().isBlank() ? it.code() : it.label())
            .map(Analyte::new).toList();
  }

  record VocabularyTerm(String code, String label, String description) {
  }

}
