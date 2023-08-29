package life.qbic.datamanager.views;

import com.vaadin.flow.component.UI;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.AppRoutes.Projects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> Handles the view elements of the {@link MainLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class MainHandler implements MainHandlerInterface {

  private MainLayout registeredMainLayout;
  private final LogoutService logoutService;

  public MainHandler(@Autowired LogoutService logoutService) {
    this.logoutService = logoutService;
  }

  @Override
  public void handle(MainLayout layout) {
    if (registeredMainLayout != layout) {
      this.registeredMainLayout = layout;
      // orchestrate view
      addClickListeners();
      // then return
    }
  }

  private void addClickListeners() {
    registeredMainLayout.homeButton().addClickListener(event -> UI.getCurrent().getPage().setLocation(
        Projects.PROJECTS));
    registeredMainLayout.logout().addClickListener(event -> logoutService.logout());
  }
}
