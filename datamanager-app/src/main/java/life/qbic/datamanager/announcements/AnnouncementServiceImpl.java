package life.qbic.datamanager.announcements;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

  private final AnnouncementRepository announcementRepository;
  private final Duration initialDelay;
  private final Duration refreshInterval;

  private static final Logger log = LoggerFactory.logger(AnnouncementServiceImpl.class);

  public AnnouncementServiceImpl(AnnouncementRepository announcementRepository,
      @Value(value = "${announcement.initial-delay}") Duration initialDelay,
      @Value(value = "${announcement.refresh-interval}") Duration refreshInterval) {
    this.announcementRepository = announcementRepository;
    this.initialDelay = initialDelay;
    this.refreshInterval = refreshInterval;
  }

  private List<Announcement> loadActiveAnnouncementsAt(Instant timePoint) {
    return announcementRepository.getAnnouncementByDisplayStartTimeBeforeAndDisplayEndTimeAfterOrderByDisplayStartTimeAsc(
            timePoint, timePoint)
        .stream()
        .map(AnnouncementServiceImpl::toApiObject)
        .distinct()
        .toList();
  }

  @Override
  public Flux<AnnouncementBundle> activeAnnouncements() {
    return Flux.interval(initialDelay, refreshInterval)
        .doOnNext(it -> log.debug("Fetching announcements"))
        .map(ignored -> loadActiveAnnouncementsAt(Instant.now()))
        .map(AnnouncementBundle::new)
        .doOnNext(it -> log.debug("Found " + it.announcements().size() + " announcements"))
        .replay(1) // every subscriber gets the last bundle directly
        .autoConnect();
  }

  private static Announcement toApiObject(AnnouncementRepository.Announcement announcement) {
    return new Announcement(announcement.getMessage());
  }
}
