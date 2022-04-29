package life.qbic.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * <b> The main view is a top-level placeholder for other views. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager ")
@Route(value = "data")
public class MainLayout extends AppLayout {

  public Button register;
  public Button login;

  private HorizontalLayout buttonLayout;
  private HorizontalLayout headerLayout;

  public MainLayout() {
    createHeaderContent();
  }

  private void createHeaderContent() {
    createHeaderLayout();
    createHeaderButtonLayout();

    addToNavbar(headerLayout, buttonLayout);
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

    buttonLayout = new HorizontalLayout(register, login);
  }

  private void styleHeaderButtons() {
    login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    buttonLayout.addClassName("button-layout-spacing");
  }
}
