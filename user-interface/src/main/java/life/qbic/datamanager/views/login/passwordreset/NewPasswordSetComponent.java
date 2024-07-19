package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
public class NewPasswordSetComponent extends CardLayout {

  @Serial
  private static final long serialVersionUID = -1138757655198857262L;

  private final Button loginButton = new Button("Login");

  public NewPasswordSetComponent() {
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Div introduction = new Div();
    introduction.add("You can now log in with your new password.");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("New Password saved!");
    add(titleSpan, introduction, loginButton);
  }

  public void addLoginButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
    loginButton.addClickShortcut(Key.ENTER);
    loginButton.addClickListener(listener);
  }
}
