package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectPreview;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple project preview repository to query concise project information
 *
 * @since 1.0.0
 */
public interface ProjectPreviewRepository extends PagingAndSortingRepository<ProjectPreview, ProjectId> {

}
