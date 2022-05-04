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
 * <b> The main view is a top-level placeholder for other views. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager ")
@Route(value = "data")
public class MainLayout extends AppLayout {

  public Button logout;

  public HorizontalLayout loggedInButtonLayout;
  private HorizontalLayout headerLayout;

  public MainLayout(@Autowired MainHandlerInterface mainHandlerInterface) {
    createHeaderContent();
    registerToHandler(mainHandlerInterface);
  }

  private void registerToHandler(MainHandlerInterface mainHandler) {
    if (mainHandler.handle(this)) {
      System.out.println("Registered main layout handler");
    } else {
      System.out.println("Already registered main layout handler");
    }
  }

  private void createHeaderContent() {
    createHeaderLayout();
    createHeaderButtonLayout();

    addToNavbar(headerLayout, loggedInButtonLayout);
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

    logout = new Button("Log out");
    loggedInButtonLayout = new HorizontalLayout(logout);

    styleHeaderButtons();
  }

  private void styleHeaderButtons() {
    loggedInButtonLayout.addClassName("button-layout-spacing");
  }
}
