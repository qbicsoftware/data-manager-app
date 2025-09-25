package life.qbic.projectmanagement.application.sync;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.dataset.LocalRawDatasetCache;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService;
import life.qbic.projectmanagement.application.sync.WatermarkRepo.Watermark;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <b>Raw Data Sync Service</b>
 *
 * <p>Service that synchronises the application state with external resources of raw datasets.</p>
 *
 * @since 1.12.0
 */
@Service
public class RawDataSyncService {

  private static final Logger log = logger(RawDataSyncService.class);
  private final RemoteRawDataService remoteRawDataService;

  private static final String JOB_NAME = "RAW_DATA_SYNC_EXTERNAL";
  // Maximum number of entries queried from remote resource
  private static final int MAX_QUERY_SIZE = 1_000;
  // Should be smaller than the lockAtMostFor duration of the scheduler lock
  private static final int MAX_DURATION_JOB_MILLIS = 10_000;

  private final WatermarkRepo watermarkRepo;
  private final LocalRawDatasetCache localRawDataService;

  @Autowired
  public RawDataSyncService(RemoteRawDataService remoteRawDataService, WatermarkRepo watermarkRepo,
      LocalRawDatasetCache localRawDatasetCache) {
    this.remoteRawDataService = Objects.requireNonNull(remoteRawDataService);
    this.watermarkRepo = Objects.requireNonNull(watermarkRepo);
    this.localRawDataService = Objects.requireNonNull(localRawDatasetCache);
  }

  // run every 2 minutes; add jitter to red@uce thundering herd on restart
  @Scheduled(fixedDelayString = "#{120 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,50)}")
  @SchedulerLock(name = "rawDataSyncJob", lockAtLeastFor = "PT20S",  // prevents instant re-acquire by another node
      lockAtMostFor = "PT40S")  // > worst-case runtime; forces unlock if node dies
  public void sync() {
    long jobStartTimeStamp = System.currentTimeMillis();
    long jobDuration = System.currentTimeMillis() - jobStartTimeStamp;
    // time box the job duration the guarantee job_duration < max_lock_duration
    // due to the stored offset in the control table, the next scheduled job will pick up there
    while (jobDuration < MAX_DURATION_JOB_MILLIS) {
      // only stay continue the job, if there are still available results to query
      if (runSync()) {
        // update milliseconds passed since start of the job
        jobDuration = System.currentTimeMillis() - jobStartTimeStamp;
      } else {
        log.debug("No more datasets available, stopping sync.");
        break;
      }
    }
  }

  /**
   * Runs the actual synchronisation and queries the remote resource from the
   * {@link RemoteRawDataService}.
   *
   * @return <code>true</code>, if there are still more datasets available to be
   * synchronised</true>, <code>false</code> if the job is complete.
   * @since 1.11.0
   */
  private boolean runSync() {
    // 1) load watermark (offset/updatedSince) from control table
    var currentWatermark = watermarkRepo.fetch(JOB_NAME)
        .orElse(new Watermark(JOB_NAME, 0, Instant.EPOCH, Instant.EPOCH));
    // 2) poll remote resource
    var result = remoteRawDataService.registeredSince(currentWatermark.updatedSince(),
        currentWatermark.syncOffset(), MAX_QUERY_SIZE);

    log.debug("Found %s new raw datasets in external resource. Syncing them now...".formatted(
        result.size()));

    // 3) persist the raw dataset information if available
    if (!result.isEmpty()) {
      log.info(
          "Persisting %d found external raw datasets in local resource. Syncing them now...".formatted(
              result.size()));
      localRawDataService.saveAll(result);
      log.info("%d raw datasets synced.".formatted(result.size()));
    }

    // 4) Create a new watermark for the next job to pick up at
    var nextWatermark = createNextWatermark(currentWatermark, result.size());

    // 5) persist the new watermark state
    watermarkRepo.save(nextWatermark);

    // 6) signal job state
    // if there were fewer results than the max query size for the search, this means
    // there are no more datasets available.
    // We can signal there are more datasets available (== true), else return false if finished
    return result.size() == MAX_QUERY_SIZE;
  }

  /**
   * There are only two meaningful updates for the newOffset:
   * <ol>
   *   <li>the result contained fewer entries then the max query size, then we are finished. So the offset can be set to 0
   * and the next job will start with a zero offset and the saved date</li>
   *  <li> the result contained as many entries as the max query size, which means that there are still more datasets to be excepted
   * or zero, if the number of datasets % query size = 0. This will lead to the first condition in the next iteration.</li>
   * </ol>
   *
   * @param currentWatermark the currently set watermark
   * @param lastResultSize   the result size seen from the last query
   * @return a new {@link Watermark} to continue at for the next job execution
   * @since 1.12.0
   */
  private static Watermark createNextWatermark(Watermark currentWatermark, int lastResultSize) {
    int newOffset =
        moreDatasetsToSync(lastResultSize, MAX_QUERY_SIZE) ? currentWatermark.syncOffset() + MAX_QUERY_SIZE : 0;
    // If the offset has been reset to 0, we can set the new watermark update since to now
    if (newOffset == 0) {
      return new Watermark(JOB_NAME, newOffset, Instant.now(), Instant.now());
    }
    // The query was not finished, so we need to query next time again from the currents watermark
    // time point BUT with the new offset to continue the paginated request
    return new Watermark(JOB_NAME, newOffset, currentWatermark.updatedSince(), Instant.now());

  }

  private static Watermark createNextFreshWatermark(Watermark currentWatermark, int lastResultSize) {}

  private static boolean moreDatasetsToSync(int lastResultSize, int maxQuerySize) {
    // if the last query returned less items than could have been based on the max query
    // size, the remote source has no more datasets to be fetched.
    // In case the results were as many as the query size, we don't know if there are more to fetch.
    // We could have accidentally hit exactly the amount of remaining datasets == max query size, or there
    // are indeed more to fetch.
    return lastResultSize == maxQuerySize;
  }
}
