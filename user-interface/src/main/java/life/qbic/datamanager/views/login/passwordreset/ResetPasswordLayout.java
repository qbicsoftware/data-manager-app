package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Objects;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.layouts.BoxLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.register.UserRegistrationLayout;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.UserNotFoundException;
import life.qbic.identity.domain.model.EmailAddress;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Reset Password")
@Route(value = AppRoutes.RESET_PASSWORD, layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class ResetPasswordLayout extends VerticalLayout {

  private final IdentityService identityService;
  public EmailField email;
  public Button sendButton;
  public Span registerSpan;
  public BoxLayout enterEmailLayout;
  public LinkSentLayout linkSentLayout;

  public ResetPasswordLayout(@Autowired IdentityService identityService) {
    this.identityService = Objects.requireNonNull(identityService);
    initLayout();
    styleLayout();
    addClickListeners();
  }


  private void initLayout() {
    initLinkSentLayout();

    initEnterEmailLayout();

    add(enterEmailLayout, linkSentLayout);
  }

  private void initEnterEmailLayout() {
    enterEmailLayout = new BoxLayout();

    enterEmailLayout.setTitleText("Reset password");
    enterEmailLayout.setDescriptionText(
        "Enter the mail address associated with your account and we'll send you a link to reset your password:");

    email = new EmailField("Email");
    enterEmailLayout.addFields(email);

    createSendButton();
    enterEmailLayout.addButtons(sendButton);

    createSpan();
    enterEmailLayout.addLinkSpanContent(registerSpan);
  }

  private void initLinkSentLayout() {
    linkSentLayout = new LinkSentLayout();
    linkSentLayout.setVisible(false);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleSendButton();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("REGISTER", UserRegistrationLayout.class);
    registerSpan = new Span(new Text("Need an account? "), link);
  }

  private void createSendButton() {
    sendButton = new Button("Send");
  }

  private void styleFieldLayout() {
    email.setWidthFull();
  }

  private void styleSendButton() {
    sendButton.setWidthFull();
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  private void addClickListeners() {
    sendButton.addClickListener(
        buttonClickEvent -> {
          clearNotifications();
          resetPassword(email.getValue());
        });
    sendButton.addClickShortcut(Key.ENTER);

    linkSentLayout.loginButton.addClickListener(
        buttonClickEvent ->
            linkSentLayout
                .getUI()
                .ifPresent(ui -> ui.navigate("login")));
  }

  private void resetPassword(String value) {
    var response = identityService.requestPasswordReset(value);
    if (response.hasFailures()) {
      onPasswordResetFailed(response);
    } else {
      onPasswordResetSucceeded();
    }
  }

  public void clearNotifications() {
    enterEmailLayout.removeNotifications();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    enterEmailLayout.setNotification(errorMessage);
  }

  private void showPasswordResetFailedError(String error, String description) {
    showError(error, description);
  }

  public void onPasswordResetSucceeded() {
    linkSentLayout.setVisible(true);
    enterEmailLayout.setVisible(false);
  }

  public void onPasswordResetFailed(ApplicationResponse response) {
    for (RuntimeException failure : response.failures()) {
      if (failure instanceof EmailAddress.EmailValidationException) {
        showPasswordResetFailedError("Invalid mail address format",
            "Please provide a valid mail address.");
      } else if (failure instanceof UserNotFoundException) {
        showPasswordResetFailedError(
            "User not found", "No user with the provided mail address is known.");
      } else if (failure instanceof IdentityService.UserNotActivatedException) {
        showPasswordResetFailedError("User not active",
            "Please activate your account first to reset the password.");
      } else {
        showPasswordResetFailedError(
            "An unexpected error occurred", "Please contact support@qbic.zendesk.com for help.");
      }
    }
  }
}
