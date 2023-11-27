package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.List;
import life.qbic.projectmanagement.application.OntologyClassEntity;
import life.qbic.projectmanagement.infrastructure.OffsetBasedRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple repository to query concise ontology term information
 *
 * @since 1.0.0
 */
public interface OntologyTermRepository extends
    PagingAndSortingRepository<OntologyClassEntity, Long> {

  @Query(value = "SELECT * FROM ontology_classes WHERE MATCH(label) AGAINST(?1 IN BOOLEAN MODE) AND ontology in (?2) ORDER BY length(label);",
      countQuery = "SELECT count(*) FROM ontology_classes WHERE MATCH(label) AGAINST(?1 IN BOOLEAN MODE) AND ontology in (?2);",
      nativeQuery = true)
  Page<OntologyClassEntity> findByLabelFulltextMatching(
      String termFilter, List<String> ontology, Pageable pageable);

  Page<OntologyClassEntity> findByLabelNotNullAndOntologyIn(List<String> ontologies, Pageable pageable);

  Page<OntologyClassEntity> findByLabelStartingWithIgnoreCaseAndOntologyIn(String filter, List<String> ontology, Pageable pageable);
}
