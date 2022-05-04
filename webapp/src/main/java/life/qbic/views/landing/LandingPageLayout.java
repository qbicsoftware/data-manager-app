package life.qbic.views.landing;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import life.qbic.views.DataManagerLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The landing page that allows logging in for the user. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
@Route(value = "landing")
public class LandingPageLayout extends DataManagerLayout {

  public Button register;
  public Button login;

  private HorizontalLayout loggedOutButtonLayout;

  public LandingPageLayout(@Autowired LandingPageHandlerInterface handlerInterface) {
    createHeaderContent();
    registerToHandler(handlerInterface);
  }

  private void registerToHandler(LandingPageHandlerInterface handler) {
    if (handler.handle(this)) {
      System.out.println("Registered main layout handler");
    } else {
      System.out.println("Already registered main layout handler");
    }
  }

  private void createHeaderContent() {
    createHeaderButtonLayout();

    addToNavbar(loggedOutButtonLayout);
  }

  private void createHeaderButtonLayout() {
    register = new Button("Register");
    login = new Button("Login");
    loggedOutButtonLayout = new HorizontalLayout(register, login);

    styleHeaderButtons();
  }

  private void styleHeaderButtons() {
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loggedOutButtonLayout.addClassName("button-layout-spacing");
  }
}
