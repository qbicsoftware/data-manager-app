package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * The page to be shown after registration when the user needs to confirm the email.
 *
 * @since 1.1.0
 */
@Route("register/pending-email-confirmation")
@PageTitle("Waiting for e-mail confirmation")
@AnonymousAllowed
public class PleaseConfirmEmailPage extends Div implements BeforeEnterObserver {

  private final UserInformationService userInformationService;

  public PleaseConfirmEmailPage(UserInformationService userInformationService) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    add(content());
  }

  private Component content() {
    HorizontalLayout horizontalLayout = new HorizontalLayout();
    horizontalLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    horizontalLayout.add(verticalLayout);
    verticalLayout.add(new H1("Please confirm your email address"));
    verticalLayout.add(
        new H2("Please confirm your email address by clicking the link we have sent you."));
    verticalLayout.add(new RouterLink("Go back to the login", LoginLayout.class));
    return horizontalLayout;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isNull(authentication)) {
      return; //logged out already
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      if (isAlreadyActiveUser(oidcUser)) {
        // no idea how they ended up here but the account is already active so they can go to the main page directly
        event.forwardTo(MainPage.class);
        return;
      }
      SecurityContextHolder.getContext().setAuthentication(null);
    }
  }

  private boolean isAlreadyActiveUser(OidcUser oidcUser) {
    Optional<UserInfo> byOidc = userInformationService.findByOidc(oidcUser.getName(),
        oidcUser.getIssuer().toString());
    return byOidc.map(UserInfo::isActive).orElse(false);
  }
}
