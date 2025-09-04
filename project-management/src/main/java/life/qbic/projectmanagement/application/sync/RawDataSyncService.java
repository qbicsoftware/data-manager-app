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
 * <p></p>
 *
 * @since 1.11.0
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
  @Scheduled(fixedDelayString = "#{10 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,2)}")
  @SchedulerLock(name = "rawDataSyncJob", lockAtLeastFor = "PT10S",  // prevents instant re-acquire by another node
      lockAtMostFor = "PT30S")  // > worst-case runtime; forces unlock if node dies
  public void sync() {
    long start = System.currentTimeMillis();
    long passedMillis = System.currentTimeMillis() - start;
    // time box the job duration the guarantee job_duration < max_lock_duration
    // due to the stored offset in the control table, the next scheduled job will pick up there
    while (passedMillis < MAX_DURATION_JOB_MILLIS) {
      // only stay continue the job, if there are still available results to query
      if (runSync()) {
        // update milliseconds passed since start of the job
        passedMillis = System.currentTimeMillis() - start;
      } else {
        log.info("No more datasets available, stopping sync.");
        break;
      }
    }
  }

  private boolean runSync() {
    // 1) load watermark (offset/updatedSince) from control table
    var currentWatermark = watermarkRepo.fetch(JOB_NAME)
        .orElse(new Watermark(JOB_NAME, 0, Instant.EPOCH, Instant.EPOCH));
    // 2) poll remote resource
    var result = remoteRawDataService.registeredSince(currentWatermark.updatedSince(),
        currentWatermark.syncOffset(), MAX_QUERY_SIZE);

    log.info("Found %s new raw datasets in external resource. Syncing them now...".formatted(
        result.size()));
    // There are only two meaningful updates for the newOffset:
    // 1. the result contained fewer entries then the max query size, then we are finished. So the offset can be set to 0
    // and the next job will start with a zero offset and the saved date
    // 2. the result contained as many entries as the max query size, which means that there are still more datasets to be excepted
    // or zero, if the number of datasets % query size = 0. This will lead to the first condition in the next iteration.
    int newOffset =
        result.size() < MAX_QUERY_SIZE ? 0 : currentWatermark.syncOffset() + MAX_QUERY_SIZE;

    // 3) persist the raw dataset information if available
    if (!result.isEmpty()) {
      log.info(
          "Persisting %d found external raw datasets in local resource. Syncing them now...".formatted(
              result.size()));
      localRawDataService.saveAll(result);
      log.info("%d raw datasets synced.".formatted(result.size()));
    }

    // 4) persist the new watermark state
    watermarkRepo.upsert(new Watermark(JOB_NAME, newOffset, Instant.now(), Instant.now()));

    // 5) signal job state
    // if there were fewer results than the max query size for the search, this means
    // there are no more datasets available.
    // We can signal the job is done (== true), else return false to continue
    return result.size() == MAX_QUERY_SIZE;
  }

}
