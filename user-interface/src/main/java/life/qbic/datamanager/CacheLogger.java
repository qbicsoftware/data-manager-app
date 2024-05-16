package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.logging.api.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CacheLogger implements CacheEventListener {

  private static final Logger log = logger(CacheLogger.class);

  @Override
  public void onEvent(CacheEvent cacheEvent) {
    log.info("Key: %s | EventType: %s | Old value: %s | New value: %s".formatted(
        cacheEvent.getKey(), cacheEvent.getType(), cacheEvent.getOldValue(),
        cacheEvent.getNewValue()));
  }
}
