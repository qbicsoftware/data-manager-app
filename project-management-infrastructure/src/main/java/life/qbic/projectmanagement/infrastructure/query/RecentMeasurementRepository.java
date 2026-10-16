package life.qbic.projectmanagement.infrastructure.query;

import java.util.List;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the {@code experiment_recent_measurement} read model.
 *
 * @since 1.19.0
 */
public interface RecentMeasurementRepository extends JpaRepository<RecentMeasurement, String> {

  List<RecentMeasurement> findAllByProjectId(String projectId);
}
