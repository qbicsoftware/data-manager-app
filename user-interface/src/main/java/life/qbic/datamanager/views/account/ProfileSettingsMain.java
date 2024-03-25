package life.qbic.datamanager.views.account;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The profile page of the user
 */
@PageTitle("Profile")
@Route(value = "profile", layout = UserMainLayout.class)
@UIScope
@PermitAll
public class ProfileSettingsMain extends Main implements BeforeEnterObserver {

  private final AccountSettingsComponent accountSettingsComponent;

  public ProfileSettingsMain(AccountSettingsComponent accountSettingsComponent) {
    this.accountSettingsComponent = accountSettingsComponent;
    add(this.accountSettingsComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      accountSettingsComponent.setUserId(qbicUserDetails.getUserId());
    } else {
      throw new ApplicationException(
          "Could not determine type of principal: " + principal);
    }
  }
}
