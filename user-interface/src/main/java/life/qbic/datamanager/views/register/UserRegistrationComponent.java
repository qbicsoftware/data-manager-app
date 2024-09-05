package life.qbic.datamanager.views.register;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.datamanager.views.general.CardLayout;
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.identity.api.UserInformationService;

/**
 * User Registration Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields with validation during user registration
 */
@SpringComponent
@UIScope
public class UserRegistrationComponent extends CardLayout {

  @Serial
  private static final long serialVersionUID = -1189104139053489520L;

  /*Validation via Binder does not show error message if an email field is used, see
   * https://github.com/vaadin/flow-components/issues/4618 for details */
  private final TextField email = new TextField("Email");

  private final PasswordField password = new PasswordField("Password");

  private final TextField fullName = new TextField("Full Name");

  private final TextField username = new TextField("Username");

  private final Button registerButton = new Button("Register");

  private final Div notificationLayout = new Div();

  private final H2 titleSpan = new H2("Register");

  private final Binder<UserRegistrationInformation> registrationInformationBinder = new Binder<>(
      UserRegistrationInformation.class);

  private final transient UserInformationService userInformationService;

  public UserRegistrationComponent(UserInformationService userInformationService) {
    this.userInformationService = Objects.requireNonNull(userInformationService);
    addClassName("user-registration-component");
    registerButton.addClassName("primary");
    username.setHelperText("Your unique user name, visible to other users");
    password.setHelperText("Please provide a password with at least 12 characters");
    add(titleSpan, notificationLayout, fullName, email, username, password, registerButton);
    setFieldValidation();
    addRegistrationButtonListener();
    addRoutingLinks();
  }

  private void addRoutingLinks() {
    RouterLink resetLink = new RouterLink("RESET", ResetPasswordMain.class);
    Span resetSpan = new Span(new Text("Forgot your password? "), resetLink);
    add(resetSpan);
  }

  private void setFieldValidation() {
    registrationInformationBinder.forField(fullName)
        .asRequired("Please provide a full name")
        .withValidator(new RegexpValidator("Please provide a valid full name", "\\S.*"))
        .bind(UserRegistrationInformation::fullName, UserRegistrationInformation::setFullName);
    registrationInformationBinder.forField(email)
        .asRequired("Please provide an email address")
        .withValidator(new EmailValidator("Please provide a valid email address"))
        .withValidator((Validator<String>) (value, context) -> {
          if (userInformationService.isEmailAvailable(value)) {
            return ValidationResult.ok();
          } else {
            return ValidationResult.error(
                "Email address already in use. "
                    + "You can reset your password if necessary");
          }
        })
        .bind(UserRegistrationInformation::email, UserRegistrationInformation::setEmail);
    registrationInformationBinder.forField(username)
        .asRequired("Please provide an user name")
        .withValidator(new RegexpValidator("Please provide a valid user name", ".*\\S+.*"))
        .withValidator((Validator<String>) (value, context) -> {
          if (userInformationService.isUserNameAvailable(value)) {
            return ValidationResult.ok();
          } else {
            return ValidationResult.error("User Name is already in use");
          }
        })
        .bind(UserRegistrationInformation::userName, UserRegistrationInformation::setUserName);
    registrationInformationBinder.forField(password)
        .asRequired("Please provide a password")
        .withValidator(
            name -> name.strip().length() >= 12,
            "Password does not contain at least 12 characters")
        .bind(UserRegistrationInformation::password, UserRegistrationInformation::setPassword);
  }


  private void addRegistrationButtonListener() {
    registerButton.addClickShortcut(Key.ENTER);
    registerButton.addClickListener(event -> {
      registrationInformationBinder.validate();
      if (registrationInformationBinder.isValid()) {
        clearNotifications();
        UserRegistrationInformation userRegistrationInformation = new UserRegistrationInformation(
            fullName.getValue().strip(), email.getValue().strip(), username.getValue().strip(),
            password.getValue().strip());
        fireEvent(
            new UserRegistrationEvent(this, event.isFromClient(), userRegistrationInformation));
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


  public void addRegistrationListener(ComponentEventListener<UserRegistrationEvent> listener) {
    addListener(UserRegistrationEvent.class, listener);
  }

  public static final class UserRegistrationInformation implements Serializable {

    @Serial
    private static final long serialVersionUID = 2143692176688070652L;
    @NotEmpty
    private String fullName = "";
    @NotEmpty
    private String email = "";
    @NotEmpty
    private String userName = "";
    @NotEmpty
    private String password = "";

    public UserRegistrationInformation(String fullName, String email, String userName,
        String password) {
      this.fullName = fullName;
      this.email = email;
      this.userName = userName;
      this.password = password;
    }

    public void setFullName(String fullName) {
      this.fullName = fullName;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String fullName() {
      return fullName;
    }

    public String email() {
      return email;
    }

    public String userName() {
      return userName;
    }

    public String password() {
      return password;
    }
  }

  public static class UserRegistrationEvent extends ComponentEvent<UserRegistrationComponent> {

    private final UserRegistrationInformation userRegistrationInformation;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UserRegistrationEvent(UserRegistrationComponent source, boolean fromClient,
        UserRegistrationInformation userRegistrationInformation) {
      super(source, fromClient);
      this.userRegistrationInformation = userRegistrationInformation;
    }

    public UserRegistrationInformation userRegistrationInformation() {
      return userRegistrationInformation;
    }
  }
}
