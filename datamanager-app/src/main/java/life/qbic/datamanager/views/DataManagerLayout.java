package life.qbic.datamanager.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Objects;
import life.qbic.datamanager.announcements.AnnouncementComponent;
import life.qbic.datamanager.announcements.AnnouncementService;
import life.qbic.datamanager.views.general.footer.FooterComponentFactory;

/**
 * <b>Data Manager Layout</b>
 *
 * <p>Defines the basic look of the application for all sites within the datamanager.
 *
 */
@PageTitle("Data Manager")
@AnonymousAllowed
public class DataManagerLayout extends AppLayout implements RouterLayout {

  private final Div contentArea;

  private static final String DRAWER_STATE_KEY = "drawerOpened";

  protected DataManagerLayout(FooterComponentFactory footerComponentFactory,
      AnnouncementService announcementService) {
    Objects.requireNonNull(footerComponentFactory);
    setId("data-manager-layout");
    // Create content area
    contentArea = new Div();
    contentArea.setId("content-area");
    AnnouncementComponent announcementComponent = new AnnouncementComponent(announcementService);
    // Add content area and footer to the main layout
    Div mainLayout = new Div(announcementComponent, contentArea, footerComponentFactory.get());
    mainLayout.setId("main-layout");
    persistDrawerStateBetweenLayouts();
    setContent(mainLayout);
    // Vaadin 25: Ensure drawer is closed by default when no content is added
    setDrawerOpened(false);
  }

  /**
   * {@inheritDoc}
   *
   * @param content
   * @throws IllegalArgumentException if content is not a {@link Component}
   */
  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentArea.removeAll();
    contentArea.getElement().appendChild(content.getElement());
  }

  /**
   * While Vaadin drawer states are persistent within the views of an individual layout
   * but not between Layouts resetting to the default state independent if a drawer was opened previously or not.
   * This method persists the drawer stages of all layouts which inherit from the {@link DataManagerLayout}
   * {@link life.qbic.datamanager.views.projects.project.ProjectMainLayout},
   * {@link life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout}
   */
  public void persistDrawerStateBetweenLayouts() {
    Boolean wasOpen = (Boolean) VaadinSession.getCurrent()
        .getAttribute(DRAWER_STATE_KEY);
    setDrawerOpened(wasOpen == null || wasOpen);
    getElement().addPropertyChangeListener(DRAWER_STATE_KEY, event -> VaadinSession.getCurrent().setAttribute(DRAWER_STATE_KEY, isDrawerOpened()));
  }
}
