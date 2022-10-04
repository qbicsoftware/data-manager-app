package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.application.ProjectPreview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple project preview repository to query concise project information
 *
 * @since 1.0.0
 */
public interface ProjectPreviewRepository extends PagingAndSortingRepository<ProjectPreview, ProjectId> {

  Page<ProjectPreview> findByProjectTitleContainingIgnoreCase(String projectTitle, Pageable pageable);
}
