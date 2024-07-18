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
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields necessary to enable a user to reset her password
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
  @Value("${routing.password-reset.reset-parameter}")
  String newPasswordParam;
  private String currentUserId;

  public SetNewPasswordMain(@Autowired IdentityService identityService,
      @Autowired SetNewPasswordComponent setNewPasswordComponent,
      @Autowired NewPasswordSetComponent newPasswordSetComponent) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.setNewPasswordComponent = Objects.requireNonNull(setNewPasswordComponent,
        "SetNewPasswordComponent cannot be null");
    this.newPasswordSetComponent = Objects.requireNonNull(newPasswordSetComponent,
        "NewPasswordSetComponent cannot be null");
    setNewPasswordComponent.addSetNewPasswordListener(this::handleSetNewPassword);
    newPasswordSetComponent.addLoginButtonListener(event -> getUI().orElseThrow().navigate(
        LoginLayout.class));
    add(setNewPasswordComponent, newPasswordSetComponent);
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
    setNewPasswordComponent.setVisible(true);
    newPasswordSetComponent.setVisible(false);
    retrieveUserIdFromUrl(beforeEvent);
  }

  public void retrieveUserIdFromUrl(BeforeEvent beforeEvent) {
    Map<String, List<String>> params = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    var resetParam = params.keySet().stream()
        .filter(entry -> Objects.equals(
            entry, newPasswordParam)).findAny();
    if (resetParam.isPresent()) {
      currentUserId = params.get(newPasswordParam).get(0);
    } else {
      throw new NotImplementedException();
    }
  }

  private void handleNewPasswordError(Result<?, RuntimeException> applicationResponse) {
    Predicate<RuntimeException> isPasswordValidationException = e -> e instanceof PasswordValidationException;
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
