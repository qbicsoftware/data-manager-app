package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
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
import java.util.Objects;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.register.UserRegistrationMain;
import life.qbic.identity.api.UserInformationService;

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
  private final transient UserInformationService userInformationService;

  public ResetPasswordComponent(UserInformationService userInformationService) {
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "userInformationService is required");
    addClassName("reset-password-component");
    confirmButton.addClassName("primary");
    Div introduction = new Div();
    introduction.add(
        "Enter the mail address associated with your account and we'll send you a link to reset your password:");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("Reset Password");
    add(titleSpan, notificationLayout, introduction, emailField, confirmButton);
    setFieldValidation();
    addRegistrationButtonListener();
    RouterLink routerLink = new RouterLink("Register", UserRegistrationMain.class);
    Span registerSpan = new Span(new Text("Don't have an account? "), routerLink);
    registerSpan.addClassName("registration-link");
    addClassName("card-layout");
    add(registerSpan);

    // Registered ONCE for the lifetime of this @UIScope bean. It forwards to whatever delegate is
    // currently set, so a new ResetPasswordMain replaces the handler instead of adding another one.
    addListener(ResetPasswordEvent.class, event -> {
      if (resetPasswordDelegate != null) {
        resetPasswordDelegate.onComponentEvent(event);
      }
    });
  }

  private void setFieldValidation() {
    emailField.setRequired(true);
    emailBinder.forField(emailField)
        .withValidator(new EmailValidator("Please provide a valid email address"))
        // There is deliberately NO "does this account exist?" validator here.
        //
        // There used to be one, and it was a synchronous, server-side account-existence oracle:
        // it answered "No user with the provided mail address is known." for an unregistered
        // address before the visitor submitted anything. That defeated the uniform response in
        // ResetPasswordMain: the service had already been fixed to answer identically for unknown
        // and unconfirmed accounts (AUTH-NFR-03, #1680/#1681), but the form disclosed the same fact
        // one step earlier — by field validation rather than by submit.
        //
        // The userInformationService is retained for construction compatibility with callers that
        // inject it, but this component must never consult it about an address.
        .bind(value -> value, (bean, value) -> {
        });
  }

  private void addRegistrationButtonListener() {
    confirmButton.addClickShortcut(Key.ENTER);
    confirmButton.addClickListener(event -> {
      clearNotifications();
      emailBinder.validate();
      if (emailBinder.isValid()) {
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

  /**
   * Registers the handler for a submitted reset request, replacing any previous one.
   *
   * <p><b>Replaces, does not accumulate.</b> This component is a {@code @UIScope} bean, so a single
   * instance is reused across every {@code ResetPasswordMain} constructed in the same UI. A plain
   * {@code addListener(...)} therefore stacks: once the view has been constructed twice, one submit
   * fires the handler twice.</p>
   *
   * <p>That was a real defect, and it silently doubled the throttle counter: submit once and the count
   * reaches 1; navigate away, come back and submit again and it lands on 3 instead of 2 — so the limit
   * was reached after half the intended number of requests. Measured: one submit = 1 increment, the
   * next submit = 2 increments.</p>
   *
   * @param listener the handler for a submit; replaces any previously registered handler
   */
  public void addResetPasswordListener(ComponentEventListener<ResetPasswordEvent> listener) {
    // Only the delegate is replaced. The event-bus registration happens exactly once, in the
    // constructor below, so repeated calls cannot stack handlers.
    this.resetPasswordDelegate = listener;
  }

  /**
   * The currently registered submit handler, so it can be removed before a new one is added.
   *
   * <p>{@code transient}: a {@link ComponentEventListener} is a server-side lambda and must not be
   * serialised into the session (AGENTS.md: Vaadin views are serialisable, but listeners holding
   * non-serialisable collaborators are not).</p>
   */
  private transient ComponentEventListener<ResetPasswordEvent> resetPasswordDelegate;

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
