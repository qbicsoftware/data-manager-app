package life.qbic.projectmanagement.infrastructure.experiment;

import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple repository to query concise ontology term information
 *
 * @since 1.0.0
 */
public interface OntologyTermRepository extends
    PagingAndSortingRepository<ProjectPreview, ProjectId> {

  Page<ProjectPreview> findByProjectTitleContainingIgnoreCaseOrProjectCodeContainingIgnoreCase(
      String projectTitle, String projectCode, Pageable pageable);
}
