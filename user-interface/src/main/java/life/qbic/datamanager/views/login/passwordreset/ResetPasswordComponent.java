package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.register.UserRegistrationMain;

/**
 * Reset Password Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields necessary to enable a user to specify his email for which
 * the password should be reset
 */
@SpringComponent
@UIScope
public class ResetPasswordComponent extends Div {

  @Serial
  private static final long serialVersionUID = 6918803421532658723L;

  /*Validation via Binder does not show error message if an email field is used, see
   * https://github.com/vaadin/flow-components/issues/4618 for details */
  private final TextField emailField = new TextField("Email");
  private final Button confirmButton = new Button("Send");
  private final Div notificationLayout = new Div();
  private final Binder<String> emailBinder = new Binder<>(String.class);
  public Span registerSpan;

  public ResetPasswordComponent() {
    addClassName("reset-password-component");
    confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div introduction = new Div();
    introduction.add(
        "Enter the mail address associated with your account and we'll send you a link to reset your password:");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("Reset Password");
    add(titleSpan, notificationLayout, introduction, emailField, confirmButton);
    setFieldValidation();
    addRegistrationButtonListener();
    addRoutingLinks();
  }

  private void addRoutingLinks() {
    RouterLink routerLink = new RouterLink("Register", UserRegistrationMain.class);
    registerSpan = new Span(new Text("Don't have an account? "), routerLink);
    registerSpan.addClassName("registration-link");
    add(registerSpan);
  }

  private void setFieldValidation() {
    emailField.setRequired(true);
    emailBinder.forField(emailField)
        .withValidator(new EmailValidator("Please provide a valid email address"))
        .bind(value -> value, (bean, value) -> {
        });
  }

  private void addRegistrationButtonListener() {
    confirmButton.addClickShortcut(Key.ENTER);
    confirmButton.addClickListener(event -> {
      emailBinder.validate();
      if (emailBinder.isValid()) {
        clearNotifications();
        fireEvent(new ResetPasswordEvent(this, event.isFromClient(), emailField.getValue()));
      }
    });
  }

  public void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
  }

  public void addResetPasswordListener(ComponentEventListener<ResetPasswordEvent> listener) {
    addListener(ResetPasswordEvent.class, listener);
  }

  public static class ResetPasswordEvent extends ComponentEvent<ResetPasswordComponent> {

    private final String email;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ResetPasswordEvent(ResetPasswordComponent source, boolean fromClient, String email) {
      super(source, fromClient);
      this.email = email;
    }

    public String getEmail() {
      return email;
    }
  }
}
