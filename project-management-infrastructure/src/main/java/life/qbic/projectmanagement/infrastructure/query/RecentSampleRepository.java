package life.qbic.projectmanagement.infrastructure.query;

import java.util.List;
import life.qbic.projectmanagement.application.query.RecentSample;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the {@code experiment_recent_sample} read model.
 *
 * @since 1.19.0
 */
public interface RecentSampleRepository extends JpaRepository<RecentSample, String> {

  List<RecentSample> findAllByProjectId(String projectId);
}
