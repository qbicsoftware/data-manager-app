package life.qbic.projectmanagement.application.concurrent;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * <b>Elastic Scheduler</b>
 * <p>
 * A scheduler for more sensitive resources, e.g. database connections or network requests
 *
 * @since 1.11.0
 */
public class ElasticScheduler {

  public static Scheduler elasticScheduler() {
    return Schedulers.boundedElastic();
  }
}
