package life.qbic.projectmanagement.infrastructure.project;

import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple project preview repository to query concise project information
 *
 * @since 1.0.0
 */
public interface ProjectPreviewRepository extends
    JpaRepository<ProjectPreview, ProjectId>,
    JpaSpecificationExecutor<ProjectPreview> {

}
