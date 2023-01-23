package life.qbic.projectmanagement.persistence.repository;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.domain.project.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.vocabulary.ControlledVocabulary;
import life.qbic.projectmanagement.domain.project.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.vocabulary.Specimen;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements ExperimentalDesignVocabularyRepository {

  private final OpenBisClient openBisClient;

  public OpenbisConnector(OpenBisClient openBisClient) {
    Objects.requireNonNull(openBisClient);
    this.openBisClient = openBisClient;
  }

  private ControlledVocabulary getVocabularyForCode(String vocabularyCode) {
    VocabularyTermSearchCriteria criteria = new VocabularyTermSearchCriteria();
    criteria.withVocabulary().withCode().thatEquals(vocabularyCode);

    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();
    SearchResult<VocabularyTerm> searchResult =
        openBisClient.getV3().searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);

    Map<String,String> termsByName = new HashMap<>();
    for (VocabularyTerm term : searchResult.getObjects()) {
      String code = term.getCode();
      String label = code;
      if (term.getLabel() != null && !term.getLabel().isEmpty()) {
        label = term.getLabel();
      }
      termsByName.put(label, code);
    }
    return new ControlledVocabulary(vocabularyCode, termsByName);
  }

  public String getCodeForVocabularyTermLabel(String vocabularyCode, String termLabel) {
    return getVocabularyForCode(vocabularyCode).getVocabularyTermCode(termLabel);
  }

  @Override
  public List<Species> retrieveOrganisms() {
    List<Species> organisms = new ArrayList<>();
    for(String name : getVocabularyForCode("Q_NCBI_TAXONOMY").getVocabularyTermNames()) {
      organisms.add(new Species(name));
    }
    return organisms;
  }

  @Override
  public List<Specimen> retrieveSpecimens() {
    List<Specimen> specimen = new ArrayList<>();
    for(String name : getVocabularyForCode("Q_PRIMARY_TISSUES").getVocabularyTermNames()) {
      specimen.add(new Specimen(name));
    }
    return specimen;
  }

  @Override
  public List<Analyte> retrieveAnalytes() {
    List<Analyte> analyte = new ArrayList<>();
    for(String name : getVocabularyForCode("Q_SAMPLE_TYPES").getVocabularyTermNames()) {
      analyte.add(new Analyte(name));
    }
    return analyte;
  }

}
