package life.qbic.datamanager.announcements;

import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.application.VirtualThreadScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

  private final AnnouncementRepository announcementRepository;

  public AnnouncementServiceImpl(AnnouncementRepository announcementRepository) {
    this.announcementRepository = announcementRepository;
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

  private static Announcement toApiObject(AnnouncementRepository.Announcement announcement) {
    return new Announcement(announcement.getMessage());
  }
}
