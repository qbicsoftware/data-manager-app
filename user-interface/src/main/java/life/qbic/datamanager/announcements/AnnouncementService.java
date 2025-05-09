package life.qbic.datamanager.announcements;

import java.time.Instant;
import reactor.core.publisher.Flux;

/**
 * Loads announcements
 */
public interface AnnouncementService {

  /**
   * A {@link Flux} containing Announcements. Only publishes announcements that are valid given the
   * provided time.
   *
   * @param timePoint the timepoint at which the announcement is valid
   * @return a {@link Flux} publishing announcements
   */
  Flux<Announcement> loadActiveAnnouncements(Instant timePoint);

  /**
   * An announcement with a given message.
   *
   * @param message
   */
  record Announcement(String message) {

  }
}
