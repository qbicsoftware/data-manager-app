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


  private final H2 titleSpan = new H2("Check your inbox");

  private final Div introduction = new Div();

  public ResetEmailSentComponent() {
    loginButton.addClassName("primary");
    // Wording matters: the same screen is shown whether or not an account exists (AUTH-NFR-03), so it
    // must not assert that a mail was definitely delivered — only what to do next.
    showSent();
    introduction.addClassName("introduction");
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

  /**
   * The "a request was accepted" state, and the only wording for it.
   *
   * <p>Deliberately conditional ("<em>if</em> an account exists"): an unknown address and a known one
   * must look identical, or this panel answers "is this address registered?" (AUTH-NFR-03).</p>
   */
  public void showSent() {
    titleSpan.setText("Check your inbox");
    introduction.setText(
        "If an account exists for that address, you will receive an email with instructions to "
            + "reset your password. Please check your inbox.");
  }

  /**
   * The "you have already asked recently" state.
   *
   * <p><b>This does not disclose whether the account exists</b>, which is why it is allowed here even
   * though this page is reachable by anyone. The throttle is keyed on the submitted string and is
   * consulted <em>before</em> any account lookup, so an unknown address is throttled exactly like a
   * known one — the message is a statement about the requester's own request volume, not about the
   * address.</p>
   *
   * <p>Staying silent instead was worse than useless: the panel promised an email that was never
   * sent, which reads as a broken mail server and lands on the helpdesk. (The <em>sent</em> wording
   * above must stay uniform; that statement genuinely would be an oracle.)</p>
   *
   * @param cooldown how long until another request will be accepted
   */
  public void showThrottled(java.time.Duration cooldown) {
    titleSpan.setText("Request already received");
    introduction.setText(
        "A reset email was requested for this address very recently. To prevent duplicate emails, "
            + "another one can be requested in %s. Please check your inbox and spam folder first."
                .formatted(cooldownText(cooldown)));
  }

  /**
   * Renders a duration for a human. Coarse on purpose: the block duration is deployment configuration
   * and copy that hard-codes "15 minutes" would drift from it.
   */
  static String cooldownText(java.time.Duration cooldown) {
    long minutes = cooldown.toMinutes();
    if (minutes < 1) {
      return "a moment";
    }
    if (minutes == 1) {
      return "1 minute";
    }
    if (minutes < 60) {
      return minutes + " minutes";
    }
    long hours = cooldown.toHours();
    return hours == 1 ? "1 hour" : hours + " hours";
  }

  /** The currently registered login handler, so it can be removed before a new one is added. */
  private transient ComponentEventListener<ClickEvent<Button>> loginDelegate;
}
