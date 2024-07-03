package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
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
@Route(value = AppRoutes.EMAIL_CONFIRMATION, layout = LandingPageLayout.class)
@PageTitle("Waiting for email confirmation")
@AnonymousAllowed
public class EmailConfirmationMain extends Main implements BeforeEnterObserver {

  private final transient UserInformationService userInformationService;

  private final Div emailConfirmationComponent = new Div();

  public EmailConfirmationMain(UserInformationService userInformationService) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    initConfirmEmailComponent();
    addClassName("email-confirmation");
    add(emailConfirmationComponent);
  }

  private void initConfirmEmailComponent() {
    emailConfirmationComponent.addClassName("email-confirmation-component");
    H2 title = new H2("Verify your email");
    emailConfirmationComponent.add(title);
    Text description = new Text(
        "We've sent a verification link to your email. Please check your inbox and click on the link to complete the registration");
    emailConfirmationComponent.add(description);
    /* Todo Unsure how this should be handled since we dispatch the email after successful registration
    Text resendVerificationText = new Text("Did not receive an email?");
    Anchor resendVerificationLink = new Anchor("", "Resend verification link");
    Span resendVerification = new Span(resendVerificationText, resendVerificationLink);
    resendVerification.addClassName("resend-verification");
    emailConfirmationComponent.add(resendVerification);
    */

    RouterLink backToLoginLink = new RouterLink("Go back to login", LoginLayout.class);
    emailConfirmationComponent.add(backToLoginLink);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isNull(authentication)) {
      return; //logged out already
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser && isAlreadyActiveUser(
        oidcUser)) {
      // no idea how they ended up here but the account is already active, so they can go to the main page directly
      event.forwardTo(MainPage.class);
    }
  }

  private boolean isAlreadyActiveUser(OidcUser oidcUser) {
    Optional<UserInfo> byOidc = userInformationService.findByOidc(oidcUser.getName(),
        oidcUser.getIssuer().toString());
    return byOidc.map(UserInfo::isActive).orElse(false);
  }
}
