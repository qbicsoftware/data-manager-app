package life.qbic.projectmanagement.application.sync;

import java.time.Instant;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface WatermarkRepo {


  record Watermark(String jobName, Instant lastUpdatedAt, Instant lastSyncedAt) {

  }
}
