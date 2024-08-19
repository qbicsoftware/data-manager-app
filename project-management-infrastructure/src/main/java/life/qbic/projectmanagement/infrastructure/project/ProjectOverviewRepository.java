package life.qbic.projectmanagement.infrastructure.project;

import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple project overview repository to search concise project information
 *
 * @since 1.0.0
 */
public interface ProjectOverviewRepository extends
    JpaRepository<ProjectOverview, ProjectId>,
    JpaSpecificationExecutor<ProjectOverview> {

}
