package life.qbic.datamanager.announcements;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.announcements.AnnouncementService.AnnouncementBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;

public class AnnouncementComponent extends Div {

  private static final Log log = LogFactory.getLog(AnnouncementComponent.class);
  private final transient AnnouncementService announcementService;


  public AnnouncementComponent(AnnouncementService announcementService) {
    this.announcementService = announcementService;
    this.setId("announcements");
    this.setVisible(false); //without subscribing to announcements nothing is displayed
  }


  private void refreshAnnouncements(AnnouncementBundle announcementBundle) {
    this.removeAll();
    this.setVisible(!announcementBundle.isEmpty());
    announcementBundle.announcements().forEach(announcement -> {
      add(renderAnnouncement(announcement));
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
    UI ui = attachEvent.getUI();
    //attach and detach to a hot strem https://vaadin.com/docs/latest/building-apps/deep-dives/presentation-layer/server-push/reactive
    Disposable announcementSubscription = announcementService.activeAnnouncements()
        .subscribe(ui.accessLater(this::refreshAnnouncements, null));

    addDetachListener(detachEvent -> {
      detachEvent.unregisterListener();
      announcementSubscription.dispose();
    });
  }
}
