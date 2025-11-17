package life.qbic.datamanager.announcements;

import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Flux;

/**
 * Loads announcements
 */
public interface AnnouncementService {

  /**
   * A {@link Flux} containing Announcements. Only publishes announcements that are valid given the
   * provided time. Published announcements are distinct until changed.
   *
   * @param timePoint the timepoint at which the announcement is valid
   * @return a {@link Flux} publishing announcements
   */
  Flux<Announcement> loadActiveAnnouncements(Instant timePoint);

  /**
   * A {@link Flux} containing announcements. Each bundle contains announcements active at the given time. The Flux is a hot source.
   * Every subscriber gets the latest 1 AnnouncementBundle and all following bundles. At least one subscription is required for the flux to connect.
   *
   * @return a hot flux publishing bundled announcements
   */
  Flux<AnnouncementBundle> activeAnnouncements();

  /**
   * An announcement with a given message.
   *
   * @param message
   */
  record Announcement(String message) {

  }

  /**
   * A bundle of announcements. The announcement list contained within is unmodifiable.
   *
   * @param announcements a list of announcements. The provided list is copied into an unmodifiable
   *                      list.
   */
  record AnnouncementBundle(List<Announcement> announcements) {

    public AnnouncementBundle {
      announcements = List.copyOf(announcements);
    }

    static AnnouncementBundle empty() {
      return new AnnouncementBundle(List.of());
    }

    public boolean isEmpty() {
      return announcements.isEmpty();
    }
  }
}
