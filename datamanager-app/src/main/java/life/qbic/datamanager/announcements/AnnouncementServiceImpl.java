package life.qbic.datamanager.announcements;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.concurrent.VirtualThreadScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

  private final AnnouncementRepository announcementRepository;
  private final Duration initialDelay;
  private final Duration refreshInterval;

  public AnnouncementServiceImpl(AnnouncementRepository announcementRepository,
      @Value(value = "${announcement.initial-delay}") Duration initialDelay,
      @Value(value = "${announcement.refresh-interval}") Duration refreshInterval) {
    this.announcementRepository = announcementRepository;
    this.initialDelay = initialDelay;
    this.refreshInterval = refreshInterval;
  }

  @Override
  public Flux<Announcement> loadActiveAnnouncements(Instant timePoint) {
    return Flux.fromIterable(
            announcementRepository.getAnnouncementByDisplayStartTimeBeforeAndDisplayEndTimeAfterOrderByDisplayStartTimeAsc(
                timePoint, timePoint))
        .distinctUntilChanged(Objects::hashCode) //avoid unnecessary work
        .map(AnnouncementServiceImpl::toApiObject)
        .subscribeOn(VirtualThreadScheduler.getScheduler());
  }

  private List<Announcement> foobar(Instant timePoint) {
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
        .map(ignored -> foobar(Instant.now()))
        .map(AnnouncementBundle::new)
        .replay(1) // every subscriber gets the last bundle directly
        .autoConnect();
  }

  private static Announcement toApiObject(AnnouncementRepository.Announcement announcement) {
    return new Announcement(announcement.getMessage());
  }
}
