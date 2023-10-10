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

  private Button homeButton;
  private Button logout;

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
    homeButton = new Button("Home");
    logout = new Button("Log out");

    return new HorizontalLayout(homeButton, logout);
  }

  public Button logout() { return logout; }

  public Button homeButton() { return homeButton; }
}
