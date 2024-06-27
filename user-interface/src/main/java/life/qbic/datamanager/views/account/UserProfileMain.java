package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.domain.model.UserId;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * User Profile Main
 * <p>
 * This component hosts the components necessary to show the profile for the current logged-in User
 * via the {@link UserProfileComponent}. Additionally, the user can see the information within his
 * {@link life.qbic.identity.api.UserInfo} and change his Username via the provided UI elements
 */

@Route(value = "profile", layout = UserMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class UserProfileMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = -5203169099713671493L;

  private static final Logger log = LoggerFactory.logger(UserProfileMain.class);
  private final UserProfileComponent userProfileComponent;
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  public UserProfileMain(@Autowired UserProfileComponent userProfileComponent,
      @Autowired UserInformationService userInformationService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userProfileComponent = requireNonNull(userProfileComponent,
        "userProfileComponent must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    addClassName("user-profile");
    add(userProfileComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userProfileComponent.getClass().getSimpleName(),
        System.identityHashCode(userProfileComponent)));
  }

  /**
   * Upon initialization of the main Component, the {@link UserProfileComponent} should be provided
   * with the {@link UserId} of the currently logged-in user
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    var userInfo = userInformationService.findById(userId).orElseThrow();
    userProfileComponent.showForUser(userInfo);
  }

}
