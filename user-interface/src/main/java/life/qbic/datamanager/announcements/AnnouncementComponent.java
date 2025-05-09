package life.qbic.datamanager.announcements;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.announcements.AnnouncementService.Announcement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class AnnouncementComponent extends Div {

  private static final Log log = LogFactory.getLog(AnnouncementComponent.class);
  private final transient AnnouncementService announcementService;
  private static final Duration INITIAL_DELAY = Duration.ZERO;
  private static final Duration REFRESH_INTERVAL = Duration.of(1, ChronoUnit.SECONDS);
  private Disposable refreshRoutine;


  public AnnouncementComponent(AnnouncementService announcementService) {
    this.announcementService = announcementService;
    this.setId("announcements");
  }

  private void subscribeToAnnouncements() {
    unsubscribeFromAnnouncements();
    UI ui = getUI().orElseThrow();
    refreshRoutine = Flux.interval(INITIAL_DELAY, REFRESH_INTERVAL)
        .doOnNext(it -> log.debug("Fetching announcements"))
        .flatMap(it -> loadActiveAnnouncements(ui)
        )
        .retryWhen(Retry.backoff(5, Duration.of(2, ChronoUnit.SECONDS))
            .doBeforeRetry(retrySignal -> log.warn("Operation failed (" + retrySignal + ")")))
        .subscribe();

  }

  private Mono<List<Announcement>> loadActiveAnnouncements(UI ui) {
    return announcementService.loadActiveAnnouncements(Instant.now())
        .distinctUntilChanged(Objects::hashCode) //avoid unnecessary work
        .collectList()
        .doOnNext(announcements -> refreshAnnouncements(announcements, ui));
  }

  private void unsubscribeFromAnnouncements() {
    if (nonNull(refreshRoutine)) {
      refreshRoutine.dispose();
    }
  }

  private void refreshAnnouncements(List<Announcement> announcements, UI ui) {
    ui.access(() -> {
      this.removeAll();
      for (Announcement announcement : announcements) {
        add(renderAnnouncement(announcement));
      }
    });
  }

  private Component renderAnnouncement(AnnouncementService.Announcement announcement) {
    Html html = new Html(
        "<div style=\"display:contents\">%s</div>".formatted(announcement.message()));
    html.addClassNames("announcement");
    return new Div(html);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    subscribeToAnnouncements();
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    unsubscribeFromAnnouncements();
    super.onDetach(detachEvent);
  }
}
