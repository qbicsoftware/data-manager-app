package life.qbic.projectmanagement.infrastructure.query;

import life.qbic.projectmanagement.application.query.ProjectSampleOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for the {@code v_project_sample_overview} read model. Specification support
 * backs the project sample list's filter (search term, experiment, measured status).
 *
 * @since 1.19.0
 */
public interface ProjectSampleOverviewRepository extends
    JpaRepository<ProjectSampleOverview, String>,
    JpaSpecificationExecutor<ProjectSampleOverview> {

}
