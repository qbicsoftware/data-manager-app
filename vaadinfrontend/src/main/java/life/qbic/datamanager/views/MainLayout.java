package life.qbic.datamanager.views;

import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import java.util.Objects;
import life.qbic.datamanager.views.navigation.ProjectNavigationDrawer;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The main view is a top-level placeholder for other views. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
public class MainLayout extends DataManagerLayout {

  private Button homeButton;
  private Button logout;

  public MainLayout(@Autowired MainHandlerInterface startHandlerInterface,
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    ProjectNavigationDrawer projectNavigationDrawer = new ProjectNavigationDrawer(
        projectInformationService,
        experimentInformationService);
    DrawerToggle drawerToggle = new DrawerToggle();
    createNavBarContent(drawerToggle);
    addToDrawer(projectNavigationDrawer);
    registerToHandler(startHandlerInterface);
  }

  private void registerToHandler(MainHandlerInterface startHandler) {
    startHandler.handle(this);
  }

  private void createNavBarContent(DrawerToggle drawerToggle) {
    addToNavbar(drawerToggle, createHeaderButtonLayout());
  }

  private HorizontalLayout createHeaderButtonLayout() {
    homeButton = new Button("Home");
    logout = new Button("Log out");

    return new HorizontalLayout(homeButton, logout);
  }

  public Button logout() {
    return logout;
  }

  public Button homeButton() {
    return homeButton;
  }
}
