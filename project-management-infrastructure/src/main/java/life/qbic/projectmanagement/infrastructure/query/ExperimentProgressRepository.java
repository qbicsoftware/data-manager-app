package life.qbic.projectmanagement.infrastructure.query;

import java.util.List;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the {@code experiment_progress} read model.
 *
 * @since 1.19.0
 */
public interface ExperimentProgressRepository extends JpaRepository<ExperimentProgress, String> {

  List<ExperimentProgress> findAllByProjectId(String projectId);
}
