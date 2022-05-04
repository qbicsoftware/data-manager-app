package life.qbic.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The landing page that allows logging in for the user. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
@Route(value = "landing")
public class LandingPageLayout extends AppLayout {

  public Button register;
  public Button login;

  public HorizontalLayout loggedOutButtonLayout;
  private HorizontalLayout headerLayout;

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
    createHeaderLayout();
    createHeaderButtonLayout();

    addToNavbar(headerLayout, loggedOutButtonLayout);
  }

  private void createHeaderLayout() {
    H1 appName = styleHeaderTitle();
    headerLayout = new HorizontalLayout(appName);

    styleHeaderLayout();
  }

  private void styleHeaderLayout() {
    headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
    headerLayout.setWidth("100%");
    headerLayout.addClassNames("py-0", "px-m");
  }

  private H1 styleHeaderTitle() {
    H1 appName = new H1("Data Manager");
    appName.addClassNames("text-l", "m-m");
    return appName;
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
