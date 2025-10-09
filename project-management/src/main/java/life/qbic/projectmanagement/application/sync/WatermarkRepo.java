package life.qbic.projectmanagement.application.sync;

import java.time.Instant;
import java.util.Optional;

/**
 * <b>Watermark Repo</b>
 *
 * <p>The watermark repo enables scheduled jobs to pick up information from a failed or incremental
 * execution and save the state after successful execution.</p>
 *
 * @since 1.11.0
 */
public interface WatermarkRepo {

  /**
   * Fetches an available {@link Watermark} for a given job name.
   *
   * @param jobName the job's unique name
   * @return a {@link Watermark} entry if available, else will return {@link Optional#empty()}
   * @since 1.11.0
   */
  Optional<Watermark> fetch(String jobName);

  /**
   * Saves the provided {@link Watermark} to the repository.
   *
   * @param latestWatermark the latest {@link Watermark} for the job
   * @since 1.11.0
   */
  void save(Watermark latestWatermark);

  /**
   * Describes a job's last successful execution state.
   *
   * @param jobName       the job's unique name
   * @param syncOffset    in incremental jobs an offset can be set to inform about where to pick up
   *                      on next execution
   * @param updatedSince  timepoint in the past to consider for the job execution
   * @param lastSuccessAt last timepoint the job finished successfully
   * @since 1.11.0
   */
  record Watermark(String jobName, int syncOffset, Instant updatedSince, Instant lastSuccessAt) {

  }
}
