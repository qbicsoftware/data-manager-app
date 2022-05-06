package life.qbic.views.landing;

import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Component;

/**
 * <b> Handles the view elements of the {@link LandingPageLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LandingPageHandler implements LandingPageHandlerInterface {

  private LandingPageLayout registeredLandingPage;

  @Override
  public void handle(LandingPageLayout layout) {
    if (registeredLandingPage != layout) {
      this.registeredLandingPage = layout;
      addClickListeners();
    }
  }

  private void addClickListeners() {

    registeredLandingPage.login.addClickListener(
        event -> UI.getCurrent().navigate("login"));

    registeredLandingPage.register.addClickListener(
        event -> UI.getCurrent().navigate("register"));

  }
}
