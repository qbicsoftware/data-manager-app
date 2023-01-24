package life.qbic.projectmanagement.persistence.repository;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import life.qbic.openbis.openbisclient.OpenBisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenbisClientAdapter implements IOpenBisClient {

  private OpenBisClient openBisClient;
  @Autowired
  public OpenbisClientAdapter(@Value("${openbis.user.username}") String username, @Value("${openbis.user.password}") String password, @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(username, password, url);
  }

  @Override
  public SearchResult<VocabularyTerm> searchVocabularyTerms(VocabularyTermSearchCriteria criteria,
      VocabularyTermFetchOptions options) {
    return openBisClient.getV3().searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);
  }
}
