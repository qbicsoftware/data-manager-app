-- ============================================================================
-- SEED SCRIPT: ip_measurement_sample_summary
-- ============================================================================
-- PURPOSE:
-- Pre-populates the `ip_measurement_sample_summary` table with existing data
-- from `specific_measurement_metadata_ip` and `sample` tables.
--
-- WHEN TO RUN:
-- Run this script EXACTLY ONCE after applying the `sql/complete-schema.sql`
-- update that introduces the `ip_measurement_sample_summary` table.
-- 
-- WHY:
-- The application's `LocalRawDatasetCache` automatically updates this summary
-- table for *new* data synced from OpenBIS via triggers/background jobs.
-- However, it does not retroactively process historical data. This script
-- bridges that gap, ensuring the optimized `v_ip_measurement_sample_json` view
-- is instantly fast for all existing measurements.
-- ============================================================================

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
GROUP BY smm.measurement_id
ON DUPLICATE KEY UPDATE 
    samples_json = VALUES(samples_json),
    experiment_ids = VALUES(experiment_ids),
    experiment_count = VALUES(experiment_count),
    min_experiment_id = VALUES(min_experiment_id),
    sample_count = VALUES(sample_count);

-- Optional: Verify the seed was successful
-- SELECT COUNT(*) AS total_summarized_measurements FROM ip_measurement_sample_summary;
