package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.List;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple repository to search concise ontology term information
 *
 * @since 1.0.0
 */
public interface OntologyTermRepositoryJpaInterface extends
    PagingAndSortingRepository<OntologyClass, Long> {

  @Query(value = "SELECT *, MATCH(label) AGAINST(?1 IN BOOLEAN MODE) relevancy FROM ontology_classes WHERE MATCH(label) AGAINST(?1 IN BOOLEAN MODE) AND ontology in (?2) ORDER BY relevancy desc, length(label);",
      countQuery = "SELECT count(*), MATCH(label) AGAINST(?1 IN BOOLEAN MODE) relevancy FROM ontology_classes WHERE MATCH(label) AGAINST(?1 IN BOOLEAN MODE) AND ontology in (?2) ORDER BY relevancy desc, length(label);",
      nativeQuery = true)
  Page<OntologyClass> findByLabelFulltextMatching(
      String termFilter, List<String> ontologyAbbreviations, Pageable pageable);

  @Query(value = "SELECT * FROM ontology_classes WHERE MATCH(name) AGAINST(?1 IN BOOLEAN MODE);",
      nativeQuery = true)
  List<OntologyClass> findByCuriFulltextMatching(String ontologyCURI);

  List<OntologyClass> findOntologyClassEntitiesByCurie(String name);

  @Query(value = "SELECT DISTINCT ontology from ontology_classes;", nativeQuery = true)
  List<String> findUniqueOntologies();
}
