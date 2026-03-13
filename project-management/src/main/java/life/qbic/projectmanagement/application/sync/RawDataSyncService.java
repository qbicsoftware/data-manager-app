package life.qbic.projectmanagement.application.sync;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataset;
import life.qbic.projectmanagement.application.dataset.LocalRawDatasetCache;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService;
import life.qbic.projectmanagement.application.sync.WatermarkRepo.Watermark;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

  private static final String JOB_NAME = "RAW_DATA_SYNC_EXTERNAL";
  // Maximum number of retry attempts on transient lock failures before giving up the iteration
  private static final int MAX_RETRY_ATTEMPTS = 3;
  // Initial backoff in milliseconds; doubles on every consecutive retry attempt
  private static final long INITIAL_BACKOFF_MS = 1_000;

  private final RemoteRawDataService remoteRawDataService;
  private final WatermarkRepo watermarkRepo;
  private final LocalRawDatasetCache localRawDataService;

  // Maximum number of entries queried from remote resource per iteration.
  // Configurable to allow operators to reduce batch size under high database load.
  private final int maxQuerySize;

  // Should be smaller than the lockAtMostFor duration of the scheduler lock.
  // Configurable to allow tuning the sync window without redeployment.
  private final int maxDurationJobMillis;

  // Self-reference injected via proxy to ensure @Transactional on runSync() is honoured.
  // Setter injection with @Lazy is required to break the circular dependency that Spring would
  // otherwise detect at startup. Cannot be injected via the primary constructor for this reason.
  private RawDataSyncService self;

  public RawDataSyncService(
      RemoteRawDataService remoteRawDataService,
      WatermarkRepo watermarkRepo,
      LocalRawDatasetCache localRawDatasetCache,
      @Value("${qbic.sync.raw-data.batch-size:1000}") int maxQuerySize,
      @Value("${qbic.sync.raw-data.max-duration-ms:10000}") int maxDurationJobMillis) {
    this.remoteRawDataService = Objects.requireNonNull(remoteRawDataService);
    this.watermarkRepo = Objects.requireNonNull(watermarkRepo);
    this.localRawDataService = Objects.requireNonNull(localRawDatasetCache);
    this.maxQuerySize = maxQuerySize;
    this.maxDurationJobMillis = maxDurationJobMillis;
  }

  @Autowired
  @Lazy
  void setSelf(RawDataSyncService self) {
    this.self = Objects.requireNonNull(self);
  }

  // run every 2 minutes; add jitter to reduce thundering herd on restart
  @Scheduled(fixedDelayString = "#{120000 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,50000)}")
  @SchedulerLock(name = "rawDataSyncJob", lockAtLeastFor = "PT20S",  // prevents instant re-acquire by another node
      lockAtMostFor = "PT40S")  // > worst-case runtime; forces unlock if node dies
  public void sync() {
    long jobStartTimeStamp = System.currentTimeMillis();
    long jobDuration = 0;
    // time box the job duration to guarantee job_duration < max_lock_duration
    // due to the stored offset in the control table, the next scheduled job will pick up there
    while (jobDuration < maxDurationJobMillis) {
      // only continue the job if there are still available results to query
      if (runSyncWithRetry()) {
        // update milliseconds passed since start of the job
        jobDuration = System.currentTimeMillis() - jobStartTimeStamp;
      } else {
        log.debug("No more datasets available, stopping sync.");
        break;
      }
    }
  }

  /**
   * Wraps {@link #runSync()} with retry logic for handling transient database lock failures.
   *
   * <p>When InnoDB lock wait timeouts or similar pessimistic locking exceptions occur (e.g. due
   * to concurrent access or a long-running manual transaction on {@code sync_control}), this
   * method retries with exponential backoff up to {@value #MAX_RETRY_ATTEMPTS} times before
   * giving up and letting the next scheduled run continue from the last committed watermark.</p>
   *
   * @return {@code true} if there are more datasets to sync, {@code false} if the iteration is
   *         complete or all retry attempts were exhausted
   * @since 1.12.0
   */
  private boolean runSyncWithRetry() {
    long backoffMs = INITIAL_BACKOFF_MS;

    for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
      try {
        return self.runSync();
      } catch (PessimisticLockingFailureException e) {
        if (attempt == MAX_RETRY_ATTEMPTS) {
          log.error(
              "Sync failed after %d attempts due to lock contention. Will retry on next scheduled run: %s"
                  .formatted(MAX_RETRY_ATTEMPTS, e.getMessage()));
          // Do not rethrow — let the next scheduled run pick up from the last committed watermark
          return false;
        }
        log.warn("Lock contention on sync attempt %d/%d, retrying in %dms".formatted(
            attempt, MAX_RETRY_ATTEMPTS, backoffMs));
        sleep(backoffMs);
        backoffMs *= 2; // exponential backoff
      }
    }
    return false;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Sync retry sleep interrupted");
    }
  }

  /**
   * Runs one synchronisation iteration and queries the remote resource via
   * {@link RemoteRawDataService}.
   *
   * <p>The dataset cache write and watermark update are wrapped in a single
   * {@link Propagation#REQUIRES_NEW} transaction so that both operations succeed or fail
   * atomically. This prevents a situation where datasets are persisted but the watermark is
   * not updated (or vice versa) due to a mid-iteration failure.</p>
   *
   * @return {@code true} if there are still more datasets available to be synchronised,
   *         {@code false} if the job is complete.
   * @since 1.11.0
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean runSync() {
    // 1) load watermark (offset/updatedSince) from control table
    var currentWatermark = watermarkRepo.fetch(JOB_NAME)
        .orElse(new Watermark(JOB_NAME, 0, Instant.EPOCH, Instant.EPOCH));
    // 2) capture the query time BEFORE issuing the remote call so the watermark never skips
    // datasets that are registered on the remote between the query and the watermark save.
    Instant queryTime = Instant.now();
   
    log.debug("Sync iteration started: offset=%d, updatedSince=%s".formatted(
        currentWatermark.syncOffset(), currentWatermark.updatedSince()));
    
     // 3) poll remote resource
    var result = remoteRawDataService.registeredSince(currentWatermark.updatedSince(),
        currentWatermark.syncOffset(), maxQuerySize);

    log.debug("Found %d new raw datasets in external resource.".formatted(result.size()));

    // 4) persist the raw dataset information if available
    if (!result.isEmpty()) {
      log.info("Persisting %d found external raw datasets in local resource.".formatted(
          result.size()));
      localRawDataService.saveAll(result);
      log.info("%d raw datasets synced.".formatted(result.size()));
    }

    // 5) Create a new watermark for the next job to pick up at
    var nextWatermark = createNextWatermark(currentWatermark, result, queryTime);

    // 6) persist the new watermark state
    watermarkRepo.save(nextWatermark);

    log.debug("Sync iteration completed: fetched=%d, newOffset=%d".formatted(
        result.size(), nextWatermark.syncOffset()));
    // 7) signal job state
    // if there were fewer results than the max query size for the search, this means
    // there are no more datasets available.
    // We can signal there are more datasets available (== true), else return false if finished
    return result.size() == maxQuerySize;
  }

  /**
   * Computes the next watermark after a sync iteration.
   *
   * <p>There are two cases:
   * <ol>
   *   <li><b>Full page</b> ({@code result.size() == MAX_QUERY_SIZE}): more pages may exist.
   *   Advance the offset by {@code MAX_QUERY_SIZE} and keep {@code updatedSince} unchanged so the
   *   next call fetches the following page of the same time window.</li>
   *   <li><b>Last page</b> (result is smaller than {@code MAX_QUERY_SIZE}, including empty): all
   *   datasets in the current time window have been consumed. Reset offset to 0 and set
   *   {@code updatedSince} to {@code queryTime} — the timestamp captured <em>before</em> the
   *   remote call was issued — so that any dataset registered on the remote between the query and
   *   the watermark save is included in the next sync window rather than silently skipped.</li>
   * </ol>
   *
   * @param currentWatermark the currently set watermark
   * @param result           the result list from the last query
   * @param queryTime        the {@link Instant} captured immediately before the remote query was
   *                         issued; must be passed from {@code runSync()} to avoid a race where
   *                         the watermark jumps past datasets registered after the query but
   *                         before {@code Instant.now()} would otherwise be called here
   * @return a new {@link Watermark} to continue at for the next job execution
   * @since 1.12.0
   */
   private Watermark createNextWatermark(Watermark currentWatermark,
       List<RawDataset> result, Instant queryTime) {
     boolean morePages = moreDatasetsToSync(result.size(), maxQuerySize);

     if (morePages) {
       // Still paginating — advance offset, keep updatedSince unchanged
       int newOffset = currentWatermark.syncOffset() + maxQuerySize;
       return new Watermark(JOB_NAME, newOffset, currentWatermark.updatedSince(), Instant.now());
     }
     // Last page (empty or partial) — all pages consumed. Use queryTime (captured before the
     // remote call) as updatedSince so the next scheduled job does not skip datasets that were
     // registered on the remote between the query and this watermark save.
     return new Watermark(JOB_NAME, 0, queryTime, Instant.now());
   }
 

  private static boolean moreDatasetsToSync(int lastResultSize, int maxQuerySize) {
    // if the last query returned fewer items than could have been based on the max query
    // size, the remote source has no more datasets to be fetched.
    // In case the results were as many as the query size, we don't know if there are more to fetch.
    // We could have accidentally hit exactly the amount of remaining datasets == max query size, or there
    // are indeed more to fetch.
    return lastResultSize == maxQuerySize;
  }
}
