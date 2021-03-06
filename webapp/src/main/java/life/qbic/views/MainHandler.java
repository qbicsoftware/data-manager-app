package life.qbic.views;

import life.qbic.security.SecurityService;
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
  private final SecurityService securityService;

  public MainHandler(@Autowired SecurityService securityService) {
    this.securityService = securityService;
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
    registeredMainLayout.logout.addClickListener(event -> securityService.logout());
  }
}
