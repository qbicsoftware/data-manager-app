package life.qbic.datamanager.announcements;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinSession;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.datamanager.announcements.AnnouncementService.Announcement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public class AnnouncementComponent extends Div {

  private static final Log log = LogFactory.getLog(AnnouncementComponent.class);
  private final transient AnnouncementService announcementService;
  private final Duration initialDelay;
  private final Duration refreshInterval;
  private transient Disposable refreshRoutine;


  public AnnouncementComponent(AnnouncementService announcementService, Duration initialDelay,
      Duration refreshInterval) {
    this.announcementService = announcementService;
    this.setId("announcements");
    this.setVisible(false); //without subscribing to announcements nothing is displayed
    this.initialDelay = initialDelay;
    this.refreshInterval = refreshInterval;
  }

  private void subscribeToAnnouncements() {
    unsubscribeFromAnnouncements();
    UI ui = getUI().orElseThrow();
    refreshRoutine = Flux.interval(initialDelay, refreshInterval)
        .doOnNext(it -> ui.access(() -> {
          int uiId = ui.getUIId();
          String pushId = Optional.ofNullable(ui.getSession())
              .map(VaadinSession::getPushId)
              .orElse(
                  "N/A");
          String sessionId = Optional.ofNullable(ui.getSession())
              .map(s -> s.getSession().getId())
              .orElse("N/A");
          log.debug(
              "Fetching announcements for ui[%s] vaadin[%s] http[%s] ".formatted(uiId, pushId,
                  sessionId));
        }))
        .flatMap(it -> announcementService.loadActiveAnnouncements(Instant.now())
            .collectList())
        .subscribe(announcements -> refreshAnnouncements(announcements, ui));
  }

  private void unsubscribeFromAnnouncements() {
    if (nonNull(refreshRoutine)) {
      refreshRoutine.dispose();
    }
  }

  private void refreshAnnouncements(List<Announcement> announcements, UI ui) {
    ui.access(() -> {
      this.removeAll();
      this.setVisible(!announcements.isEmpty());
      for (Announcement announcement : announcements) {
        add(renderAnnouncement(announcement));
      }
    });
  }

  private Component renderAnnouncement(AnnouncementService.Announcement announcement) {
    Html html = new Html(
        "<div class=\"announcement-text\">%s</div>".formatted(announcement.message()));
    Div div = new Div(VaadinIcon.WRENCH.create(), html);
    div.addClassNames("announcement");
    return div;
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
