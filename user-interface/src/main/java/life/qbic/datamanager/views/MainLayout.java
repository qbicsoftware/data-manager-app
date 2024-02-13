package life.qbic.datamanager.views;

import com.vaadin.flow.router.PageTitle;
import java.util.Objects;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.general.DataManagerMenu;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The main view is a top-level placeholder for other views. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
public class MainLayout extends DataManagerLayout {

  private final DataManagerMenu dataManagerMenu;

  public MainLayout(@Autowired LogoutService logoutService) {
    Objects.requireNonNull(logoutService);
    dataManagerMenu = new DataManagerMenu(logoutService);
    addToNavbar(dataManagerMenu);
    addClassName("main-layout");
  }
}
