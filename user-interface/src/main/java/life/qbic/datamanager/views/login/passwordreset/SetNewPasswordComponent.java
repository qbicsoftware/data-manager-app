package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.general.CardLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;

/**
 * Set New Password Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields necessary to enable a user to reset her password
 */
@AnonymousAllowed
@UIScope
@SpringComponent
public class SetNewPasswordComponent extends CardLayout {

  @Serial
  private static final long serialVersionUID = 4482422913026333378L;

  private final PasswordField password = new PasswordField("Password");

  private final Button confirmButton = new Button("Confirm");

  private final Div notificationLayout = new Div();

  private final Binder<String> passwordResetBinder = new Binder<>(String.class);

  public SetNewPasswordComponent() {
    confirmButton.addClassName("primary");
    password.setHelperText("Please provide a password with at least 12 characters");
    Div introduction = new Div();
    introduction.add("Please provide a new password for your account:");
    introduction.addClassName("introduction");
    H2 titleSpan = new H2("Set New Password");
    add(titleSpan, notificationLayout, introduction, password, confirmButton);
    setFieldValidation();
    addConfirmButtonListeners();
  }

  private void setFieldValidation() {
    passwordResetBinder.forField(password)
        .asRequired("Please provide a password")
        .withValidator(
            name -> name.strip().length() >= 12,
            "Password is too short")
        .bind(value -> value, (bean, value) -> {
        });
  }

  private void addConfirmButtonListeners() {
    confirmButton.addClickShortcut(Key.ENTER);
    confirmButton.addClickListener(event -> {
      passwordResetBinder.validate();
      if (passwordResetBinder.isValid()) {
        clearNotifications();
        fireEvent(new SetNewPasswordEvent(this, event.isFromClient(), password.getValue()));
      }
    });
  }

  private void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
  }

  public void addSetNewPasswordListener(ComponentEventListener<SetNewPasswordEvent> listener) {
    addListener(SetNewPasswordEvent.class, listener);
  }

  public static class SetNewPasswordEvent extends ComponentEvent<SetNewPasswordComponent> {

    private final String password;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public SetNewPasswordEvent(SetNewPasswordComponent source, boolean fromClient,
        String password) {
      super(source, fromClient);
      this.password = password;
    }

    public String getPassword() {
      return password;
    }
  }
}
