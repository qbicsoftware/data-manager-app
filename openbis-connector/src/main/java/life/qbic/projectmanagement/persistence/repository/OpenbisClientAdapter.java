package life.qbic.projectmanagement.persistence.repository;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import life.qbic.openbis.openbisclient.OpenBisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenbisClientAdapter implements IOpenBisClient {

  private final OpenBisClient openBisClient;

  public OpenbisClientAdapter(@Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(
        userName, password, url);
    System.out.println(userName);
    System.out.println(url);
    System.out.println(password);
    openBisClient.login();
  }

  @Override
  public SearchResult<VocabularyTerm> searchVocabularyTerms(VocabularyTermSearchCriteria criteria,
      VocabularyTermFetchOptions options) {
    return openBisClient.getV3()
        .searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);
  }
}
