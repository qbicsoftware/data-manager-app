package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.general.CardLayout;

/**
 * Reset Email Sent Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component. Informing the user that a reset password email was sent and directing her to the login
 * layout
 */
@SpringComponent
@UIScope
public class ResetEmailSentComponent extends CardLayout {

  @Serial
  private static final long serialVersionUID = -1138757655198857262L;

  private final Button loginButton = new Button("Login");

  public ResetEmailSentComponent() {
    loginButton.addClassName("primary");
    Div introduction = new Div();
    introduction.add("Please check your inbox and follow the instructions to reset your password.");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("Email has been sent");
    add(titleSpan, introduction, loginButton);
  }

  public void addLoginButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
    loginButton.addClickListener(listener);
  }
}
