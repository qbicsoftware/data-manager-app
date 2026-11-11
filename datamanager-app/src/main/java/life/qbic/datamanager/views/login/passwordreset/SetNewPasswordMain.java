package life.qbic.datamanager.views.login.passwordreset;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLink;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.datamanager.views.login.passwordreset.SetNewPasswordComponent.SetNewPasswordEvent;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.domain.model.EncryptedPassword.PasswordValidationException;
import life.qbic.logging.api.Logger;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Set New Password Main
 * <p>
 * Main component hosting the components necessary for a user to reset her password {@link SetNewPasswordComponent} a
 * and informing her that her provided input is now set as a new password for her account
 * {@link NewPasswordSetComponent}
 * component.
 */
@PageTitle("New Password")
@Route(value = AppRoutes.NEW_PASSWORD, layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
@SpringComponent
public class SetNewPasswordMain extends Main implements HasUrlParameter<String> {

  private static final Logger log = logger(SetNewPasswordMain.class);
  @Serial
  private static final long serialVersionUID = 2083979684773021467L;

  private final SetNewPasswordComponent setNewPasswordComponent;
  private final NewPasswordSetComponent newPasswordSetComponent;
  private final transient IdentityService identityService;

  private final transient life.qbic.identity.api.VerificationLinkService verificationLinkService;

  @Value("${routing.password-reset.token-parameter:token}")
  String tokenParameter;

  /**
   * The account the token belongs to, resolved from the token on entry.
   *
   * <p>Deliberately <b>not</b> read from a URL parameter: a user id in a reset URL is a permanent,
   * guessable credential that also appears in ACL records. The user is established by consuming the
   * token, and only then does the password form become usable (AUTH-R-05).</p>
   */
  private String currentUserId;

  /** True only after a successful token consumption; gates the password change. */
  private boolean resetAuthorised;

  /**
   * Shown when the link cannot be used. Exists because the rejection path used to hide both
   * components and render nothing at all — the page went blank with only a log line, which is
   * indistinguishable from "still loading" and reports nothing to the user.
   */
  private final Div invalidLinkComponent = new Div();

  public SetNewPasswordMain(@Autowired IdentityService identityService,
      @Autowired SetNewPasswordComponent setNewPasswordComponent,
      @Autowired NewPasswordSetComponent newPasswordSetComponent,
      @Autowired life.qbic.identity.api.VerificationLinkService verificationLinkService) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.verificationLinkService = Objects.requireNonNull(verificationLinkService,
        "verificationLinkService cannot be null");
    this.setNewPasswordComponent = Objects.requireNonNull(setNewPasswordComponent,
        "SetNewPasswordComponent cannot be null");
    this.newPasswordSetComponent = Objects.requireNonNull(newPasswordSetComponent,
        "NewPasswordSetComponent cannot be null");
    setNewPasswordComponent.addSetNewPasswordListener(this::handleSetNewPassword);
    newPasswordSetComponent.addLoginButtonListener(event -> getUI().orElseThrow().navigate(
        LoginLayout.class));
    initInvalidLinkComponent();
    add(setNewPasswordComponent, newPasswordSetComponent, invalidLinkComponent);
    addClassName("set-new-password");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        setNewPasswordComponent.getClass().getSimpleName(),
        System.identityHashCode(setNewPasswordComponent),
        newPasswordSetComponent.getClass().getSimpleName(),
        System.identityHashCode(newPasswordSetComponent)));
  }

  private void handleSetNewPassword(SetNewPasswordEvent event) {
    if (!resetAuthorised || currentUserId == null) {
      // Defence in depth: the form must never be usable without a consumed token, even if the
      // component were somehow rendered directly.
      setNewPasswordComponent.setVisible(false);
      newPasswordSetComponent.setVisible(false);
      return;
    }
    var result = identityService.newUserPassword(currentUserId,
        event.getPassword().toCharArray());
    if (result.isValue()) {
      setNewPasswordComponent.setVisible(false);
      newPasswordSetComponent.setVisible(true);
    }
    if (result.isError()) {
      handleNewPasswordError(result);
    }
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
    newPasswordSetComponent.setVisible(false);
    invalidLinkComponent.setVisible(false);
    resetAuthorised = false;
    currentUserId = null;

    // The token is consumed here, on entry. Unlike email confirmation this is acceptable: showing
    // the form IS the user acting on the link, and a scanner cannot set a password because that
    // still needs form input. Consuming also means a prefetched or replayed link is already dead
    // before anyone could type into it.
    String rawToken = extractToken(beforeEvent);
    java.util.Optional<String> userId = rawToken == null
        ? java.util.Optional.empty()
        : verificationLinkService.consume(rawToken,
            life.qbic.identity.api.VerificationLinkService.Purpose.PASSWORD_RESET,
            java.time.Instant.now());

    if (userId.isEmpty()) {
      // One message for every rejection: unknown, expired, already used, wrong flow (AUTH-NFR-03).
      // The user-facing wording must not distinguish them — but the user must SEE something.
      log.info("Password reset link rejected; token unusable, unknown, expired or already used.");
      setNewPasswordComponent.setVisible(false);
      newPasswordSetComponent.setVisible(false);
      invalidLinkComponent.setVisible(true);
      return;
    }
    currentUserId = userId.get();
    resetAuthorised = true;
    invalidLinkComponent.setVisible(false);
    setNewPasswordComponent.setVisible(true);
  }

  private void initInvalidLinkComponent() {
    invalidLinkComponent.addClassName("invalid-reset-link");
    invalidLinkComponent.add(new com.vaadin.flow.component.html.H2("This link is no longer valid"));
    invalidLinkComponent.add(new com.vaadin.flow.component.html.Paragraph(
        "Your password reset link has expired or has already been used. "
            + "Request a new one from the login page."));
    invalidLinkComponent.add(new com.vaadin.flow.component.html.Div(
        new RouterLink("Go to login", LoginLayout.class)));
    invalidLinkComponent.setVisible(false);
  }

  private String extractToken(BeforeEvent beforeEvent) {
    Map<String, List<String>> params = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    List<String> values = params.get(tokenParameter);
    return values == null || values.isEmpty() ? null : values.getFirst();
  }

  private void handleNewPasswordError(Result<?, RuntimeException> applicationResponse) {
    Predicate<RuntimeException> isPasswordValidationException = e -> e instanceof PasswordValidationException;
        /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    applicationResponse
        .onErrorMatching(isPasswordValidationException, ignored -> {
          log.error("Invalid password provided during reset");
          setNewPasswordComponent.showError("Invalid Password provided",
              "The provided password does not meet security standard");
        })
        .onErrorMatching(isPasswordValidationException.negate(), ignored -> {
          log.error("Unexpected failure on password reset for user.");
          setNewPasswordComponent.showError("An unexpected error occurred",
              "Please contact support@qbic.zendesk.com for help.");
        });
  }
}
