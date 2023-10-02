package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.UI;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.AppRoutes.Projects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> Handles the view elements of the {@link ProjectMainLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class ProjectMainHandler implements ProjectMainHandlerInterface {

  private ProjectMainLayout projectMainLayout;
  private final LogoutService logoutService;

  public ProjectMainHandler(@Autowired LogoutService logoutService) {
    this.logoutService = logoutService;
  }

  @Override
  public void handle(ProjectMainLayout layout) {
    if (projectMainLayout != layout) {
      this.projectMainLayout = layout;
      // orchestrate view
      addClickListeners();
      // then return
    }
  }

  private void addClickListeners() {
    projectMainLayout.homeButton().addClickListener(event -> UI.getCurrent().getPage().setLocation(
        Projects.PROJECTS));
    projectMainLayout.logout().addClickListener(event -> logoutService.logout());
  }
}
