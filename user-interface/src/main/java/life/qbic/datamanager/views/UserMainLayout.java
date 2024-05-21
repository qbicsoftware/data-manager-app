package life.qbic.datamanager.views;

import com.vaadin.flow.router.PageTitle;
import java.util.Objects;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.general.DataManagerMenu;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.identity.api.UserInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> The user main layout is the layout holding all views outside of an individual project view,
 * such as the personal-access-token-management {@link PersonalAccessTokenMain} and listing the all
 * projects for a user {@link ProjectOverviewMain}</b>
 *
 * @since 1.0.0
 */
@PageTitle("Data Manager")
public class UserMainLayout extends DataManagerLayout {

  private final DataManagerMenu dataManagerMenu;

  public UserMainLayout(@Autowired LogoutService logoutService,
      UserInformationService userInformationService) {
    Objects.requireNonNull(logoutService);
    Objects.requireNonNull(userInformationService);
    dataManagerMenu = new DataManagerMenu(logoutService, userInformationService);
    addToNavbar(dataManagerMenu);

  }
}
