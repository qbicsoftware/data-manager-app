package life.qbic.datamanager.views.login.newpassword;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.layouts.BoxLayout;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.domain.model.EncryptedPassword.PasswordValidationException;
import life.qbic.logging.api.Logger;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("New Password")
@Route(value = AppRoutes.NEW_PASSWORD, layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class NewPasswordLayout extends VerticalLayout implements HasUrlParameter<String> {

  private static final Logger log = logger(NewPasswordLayout.class);

  @Serial
  private static final long serialVersionUID = 4884878964166607894L;
  private final IdentityService identityService;
  private PasswordField newPassword;
  private Button sendButton;

  private BoxLayout provideNewPasswordLayout;
  private NewPasswordSetLayout newPasswordSetLayout;

  private transient String currentUserId;

  private final NewPasswordHandler newPasswordHandler;

  public NewPasswordLayout(@Autowired NewPasswordHandler newPasswordHandler,
      @Autowired IdentityService identityService) {
    this.newPasswordHandler = Objects.requireNonNull(newPasswordHandler);
    this.identityService = Objects.requireNonNull(identityService);
    initLayout();
    styleLayout();
  }

  public PasswordField newPassword() {
    return newPassword;
  }

  public Button sendButton() {
    return sendButton;
  }

  public BoxLayout provideNewPasswordLayout() {
    return provideNewPasswordLayout;
  }

  public NewPasswordSetLayout newPasswordSetLayout() {
    return newPasswordSetLayout;
  }

  private void initLayout() {
    initPasswordSetLayout();

    initEnterEmailLayout();

    add(provideNewPasswordLayout, newPasswordSetLayout);
  }

  private void initEnterEmailLayout() {
    provideNewPasswordLayout = new BoxLayout();

    provideNewPasswordLayout.setTitleText("Set new password");
    provideNewPasswordLayout.setDescriptionText("Please provide a new password for your account:");

    newPassword = new PasswordField("Password");
    provideNewPasswordLayout.addFields(newPassword);

    createSendButton();
    provideNewPasswordLayout.addButtons(sendButton);

  }

  private void initPasswordSetLayout() {
    newPasswordSetLayout = new NewPasswordSetLayout();
    newPasswordSetLayout.setVisible(false);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleSendButton();
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
  }

  private void createSendButton() {
    sendButton = new Button("Send");
  }

  private void styleFieldLayout() {
    newPassword.setWidthFull();
  }

  private void styleSendButton() {
    sendButton.setWidthFull();
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
    handle(beforeEvent);
  }

  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> params = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    var resetParam = params.keySet().stream()
        .filter(entry -> Objects.equals(
            entry, newPasswordHandler.passwordResetQueryParameter())).findAny();
    if (resetParam.isPresent()) {
      currentUserId = params.get(newPasswordHandler.passwordResetQueryParameter()).get(0);
    } else {
      throw new NotImplementedException();
    }
  }

  private void addClickListeners() {
    sendButton().addClickListener(buttonClickEvent -> {
        Result<?, RuntimeException> applicationResponse = identityService.newUserPassword(currentUserId,
            newPassword().getValue().toCharArray());
        if (applicationResponse.isError()) {
          handleNewPasswordError(applicationResponse);
        }
        handleSuccess();
    }
    );
    sendButton().addClickShortcut(Key.ENTER);

    newPasswordSetLayout().loginButton().addClickListener(
        buttonClickEvent ->
            newPasswordSetLayout().getUI()
                .ifPresent(ui -> ui.navigate("login")));
  }

  private void handleNewPasswordError(Result<?, RuntimeException> applicationResponse) {
    Predicate<RuntimeException> isPasswordValidationException = e -> e instanceof PasswordValidationException;
    applicationResponse
        .onErrorMatching(isPasswordValidationException, ignored -> {
          log.error("Could not set new password for user.");
          onPasswordValidationFailure();
        })
        .onErrorMatching(isPasswordValidationException.negate(), ignored -> {
          log.error("Unexpected failure on password reset for user.");
          onUnexpectedFailure();
        });
  }

  private void handleSuccess() {
    onSuccessfulNewPassword();
  }

  public void onSuccessfulNewPassword() {
    provideNewPasswordLayout().setVisible(false);
    newPasswordSetLayout().setVisible(true);
  }

  public void onPasswordValidationFailure() {
    throw new UnsupportedOperationException();
  }

  public void onUnexpectedFailure() {
    throw new UnsupportedOperationException();
  }
}
