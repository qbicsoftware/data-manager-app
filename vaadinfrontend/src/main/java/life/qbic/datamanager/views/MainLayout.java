package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The main view is a top-level placeholder for other views. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
public class MainLayout extends DataManagerLayout {

  public Button logout;

  public MainLayout(@Autowired MainHandlerInterface startHandlerInterface) {
    createNavBarContent();
    registerToHandler(startHandlerInterface);
  }

  private void registerToHandler(MainHandlerInterface startHandler) {
    startHandler.handle(this);
  }

  private void createNavBarContent() {
    addToNavbar(createHeaderButtonLayout());
  }

  private HorizontalLayout createHeaderButtonLayout() {
    logout = new Button("Log out");
    HorizontalLayout loggedInButtonLayout = new HorizontalLayout(logout);
    loggedInButtonLayout.addClassName("button-layout-spacing");

    return loggedInButtonLayout;
  }
}
