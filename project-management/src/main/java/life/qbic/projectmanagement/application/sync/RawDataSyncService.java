package life.qbic.projectmanagement.application.sync;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class RawDataSyncService {

  private static final Logger log = logger(RawDataSyncService.class);

  private final AsyncProjectService projectService;

  @Autowired
  public RawDataSyncService(AsyncProjectService projectService) {
    this.projectService = Objects.requireNonNull(projectService);
  }

  // run every 2 minutes; add jitter to reduce thundering herd on restart
  @Scheduled(fixedDelayString = "#{10 + T(java.util.concurrent.ThreadLocalRandom).current().nextInt(0,1)}")
  @SchedulerLock(name = "rawDataSyncJob",
      lockAtLeastFor = "PT10S",  // prevents instant re-acquire by another node
      lockAtMostFor  = "PT30S")  // > worst-case runtime; forces unlock if node dies
  public void sync() {
    // 1) load watermark (cursor/updatedSince) from control table
    // 2) poll remote incrementally (time-box the loop, e.g. ≤ 25s)
    // 3) idempotent upsert into materialized tables
    // 4) persist new watermark + success timestamp
    log.info("Starting raw data sync service...");
  }
}
