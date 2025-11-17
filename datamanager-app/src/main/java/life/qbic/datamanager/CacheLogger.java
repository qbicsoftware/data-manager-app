package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.logging.api.Logger;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;

public class CacheLogger implements CacheEventListener<ObjectIdentityImpl, MutableAcl> {

  private static final Logger log = logger(CacheLogger.class);

  @Override
  public void onEvent(CacheEvent cacheEvent) {
    log.debug("Key: %s | EventType: %s | Old value: %s | New value: %s".formatted(
        cacheEvent.getKey(), cacheEvent.getType(), cacheEvent.getOldValue(),
        cacheEvent.getNewValue()));
  }
}
