package life.qbic.projectmanagement.application.sync;

import java.time.Instant;
import java.util.Optional;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface WatermarkRepo {

  Optional<Watermark> fetch(String jobName);

  void upsert(Watermark currentWatermark);

  record Watermark(String jobName, int syncOffset, Instant updatedSince, Instant lastSuccessAt) {

  }
}
