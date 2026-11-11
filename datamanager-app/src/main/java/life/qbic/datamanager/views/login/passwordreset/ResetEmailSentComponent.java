package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;

/**
 * Reset Email Sent Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component. Informing the user that a reset password email was sent and directing her to the login
 * layout
 */
@SpringComponent
@UIScope
public class ResetEmailSentComponent extends Div {

  @Serial
  private static final long serialVersionUID = -1138757655198857262L;

  private final Button loginButton = new Button("Login");


  public ResetEmailSentComponent() {
    loginButton.addClassName("primary");
    Div introduction = new Div();
    // Wording matters: the same screen is shown whether or not an account exists (AUTH-NFR-03), so
    // it must not assert that a mail was definitely delivered — only what to do next.
    introduction.add(
        "If an account exists for that address, you will receive an email with instructions to "
            + "reset your password. Please check your inbox.");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("Check your inbox");
    addClassName("card-layout");
    add(titleSpan, introduction, loginButton);

    // Registered ONCE for the lifetime of this @UIScope bean; forwards to the current delegate.
    loginButton.addClickListener(event -> {
      if (loginDelegate != null) {
        loginDelegate.onComponentEvent(event);
      }
    });
  }

  /**
   * Registers the handler for the login button, replacing any previous one.
   *
   * <p>Same trap as {@link ResetPasswordComponent#addResetPasswordListener}: this component is a
   * {@code @UIScope} bean, so {@code addClickListener} accumulates across view constructions and one
   * click would run every handler ever registered. The visible symptom is a navigation happening N
   * times for N page loads; the throttle counter is not affected here, but the fix is the same.</p>
   *
   * @param listener the handler for the login button; replaces any previously registered handler
   */
  public void addLoginButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
    // Only the delegate is replaced; the click registration below happens once, in the constructor.
    this.loginDelegate = listener;
  }

  /** The currently registered login handler, so it can be removed before a new one is added. */
  private transient ComponentEventListener<ClickEvent<Button>> loginDelegate;
}
