package life.qbic.projectmanagement.infrastructure.project;

import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple project preview repository to query concise project information
 *
 * @since 1.0.0
 */
public interface ProjectPreviewRepository extends
    PagingAndSortingRepository<ProjectPreview, ProjectId> {

  Page<ProjectPreview> findByProjectTitleContainingIgnoreCaseOrProjectCodeContainingIgnoreCase(
      String projectTitle, String projectCode, Pageable pageable);
}
