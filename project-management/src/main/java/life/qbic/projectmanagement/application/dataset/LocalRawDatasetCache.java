package life.qbic.projectmanagement.application.dataset;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <b>Local Raw Dataset Cache</b>
 * <p>
 * A simple cache that enables the application to persist metadata about raw dataset metadata from
 * external resources associated with measurements from the application.
 *
 * @since 1.11.0
 */
@Service
public class LocalRawDatasetCache {

  private final LocalRawDatasetRepository repository;
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public LocalRawDatasetCache(LocalRawDatasetRepository repository, JdbcTemplate jdbcTemplate) {
    this.repository = Objects.requireNonNull(repository);
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
  }

  public void saveAll(List<RawDataset> rawDataset) {
    Objects.requireNonNull(rawDataset);
    repository.saveAll(rawDataset);

    // Also update the IP summary table for any newly synced measurements
    for (RawDataset dataset : rawDataset) {
      updateIpSummary(dataset.measurementId());
    }
  }

  private void updateIpSummary(String measurementId) {
    String sql = """
        INSERT INTO ip_measurement_sample_summary 
            (measurement_id, samples_json, experiment_ids, experiment_count, min_experiment_id, sample_count)
        SELECT smm.measurement_id,
               JSON_ARRAYAGG(JSON_OBJECT('sample_id', s.sample_id, 'code', s.code, 'label', s.label) ORDER BY s.code),
               GROUP_CONCAT(DISTINCT s.experiment_id ORDER BY s.experiment_id SEPARATOR ','),
               COUNT(DISTINCT s.experiment_id),
               MIN(s.experiment_id),
               COUNT(*)
        FROM specific_measurement_metadata_ip smm
        LEFT JOIN sample s ON s.sample_id = smm.sample_id
        WHERE smm.measurement_id = ?
        GROUP BY smm.measurement_id
        ON DUPLICATE KEY UPDATE 
            samples_json = VALUES(samples_json),
            experiment_ids = VALUES(experiment_ids),
            experiment_count = VALUES(experiment_count),
            min_experiment_id = VALUES(min_experiment_id),
            sample_count = VALUES(sample_count)
        """;
    jdbcTemplate.update(sql, measurementId);
  }

}
