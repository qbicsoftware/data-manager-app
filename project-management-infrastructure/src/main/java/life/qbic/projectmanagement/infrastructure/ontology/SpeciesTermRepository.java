package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupInterface;
import life.qbic.projectmanagement.domain.repository.SpeciesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;


/**
 * <b>Ontology term repository implementation</b>
 *
 * <p>Implementation for the {@link SpeciesRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link OntologyClass} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link OntologyTermRepositoryJpaInterface}, which is
 * injected as
 * dependency upon creation.
 * <p>
 *
 * @since 1.0.0
 */
@Service
public class SpeciesTermRepository implements SpeciesRepository, SpeciesLookupInterface {

  private final OntologyTermRepositoryJpaInterface jpaRepository;

  @Autowired
  public SpeciesTermRepository(OntologyTermRepositoryJpaInterface jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  private static Sort sortByOrders(List<SortOrder> sortOrders) {
    return Sort.by(sortOrders.stream()
        .map(sortOrder -> sortOrder.isDescending()
            ? Order.desc(sortOrder.propertyName())
            : Order.asc(sortOrder.propertyName()))
        .toList());
  }

  @Override
  public Optional<OntologyClass> findByCuri(String curie) {
    return jpaRepository.findOntologyClassEntitiesByCurie(curie).stream().findAny();
  }

  /**
   * We ensure that in case of multiple words we use them as full phrase instead of single
   * individual words. The search terms are semantically belonging to the same concept the user is
   * intereseted in, e.g.
   * <p>
   * Homo sapiens instead of "Homo" and "sapiens".
   * <p>
   * So we wrap it according the operator syntax in boolean mode with quotes. The asterisk suffix is
   * needed so incomplete words are found (mu for musculus).
   */
  private String buildSearchTerm(String searchString) {
    StringBuilder searchTermBuilder = new StringBuilder();
    String[] words = searchString.split("\\s+");
    String quot = "\"";

    if (words.length > 1) {
      var completedWords = String.join(" ", Arrays.copyOf(words, words.length - 1));
      var lastWord = words[words.length-1];
      var fullTermCleaned = completedWords+" "+lastWord;

      // first part
      searchTermBuilder.append(quot).append(fullTermCleaned).append(quot).append(" < ");
      // middle part
      searchTermBuilder.append(quot).append(completedWords).append(quot).append(" < ");
      // last part
      searchTermBuilder.append("+").append(fullTermCleaned).append("*");

    } else {
      searchTermBuilder
          .append(quot).append(words[0]).append(quot).append(" < ")
          .append("+").append(words[0]).append("*");
    }
    return searchTermBuilder.toString();
  }

  @Override
  public List<OntologyClass> query(FilterTerm filterTerm,
      List<String> ontologyAbbreviations,
      int offset,
      int limit, List<SortOrder> sortOrders) {

    var searchString = filterTerm.term().trim();
    if (searchString.length() < 2) {
      return new ArrayList<>();
    }
    // otherwise create a more complex search term for fulltext search
    String searchTerm = buildSearchTerm(searchString);
    OffsetBasedRequest pageable = new OffsetBasedRequest(offset, limit, sortByOrders(sortOrders));
    return jpaRepository.findByLabelFulltextMatching(searchTerm, ontologyAbbreviations,
            pageable)
        .getContent();
  }

  @Override
  public Collection<OntologyClass> query(OntologyCurie ontologyCurie) {
    // The CURIE (aka "name" in the database) is currently formatted with an "_" (underscore) as delimiter
    var delimiterCorrectedCURI = ontologyCurie.curie().trim().replace(":", "_");
    // And the prefix is in capitalised form
    var capitalizedCURI = delimiterCorrectedCURI.toUpperCase();

    return jpaRepository.findByCuriFulltextMatching(capitalizedCURI);
  }

  public List<String> findUniqueOntologyAbbreviations() {
    return jpaRepository.findUniqueOntologies();
  }
}
