package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectPreview;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since 1.0.0
 */
public interface ProjectPreviewRepository extends PagingAndSortingRepository<ProjectPreview, ProjectId> {

}
