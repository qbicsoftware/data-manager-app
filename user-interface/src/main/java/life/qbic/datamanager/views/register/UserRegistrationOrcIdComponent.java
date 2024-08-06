package life.qbic.datamanager.views.register;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.datamanager.views.general.CardLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.identity.api.UserInformationService;

/**
 * User Registration OrcId Component
 * <p>
 * Card Stylized component similar to {@link UserRegistrationComponent} component, Providing the
 * input fields with validation during user registration if done via OrcId. This skips the password
 * validation since the user id validated via her OrcId
 */
@SpringComponent
@UIScope
public class UserRegistrationOrcIdComponent extends CardLayout {

  @Serial
  private static final long serialVersionUID = 822067456152829562L;

  /*Validation via Binder does not show error message if an email field is used, see
   * https://github.com/vaadin/flow-components/issues/4618 for details */
  private final TextField email = new TextField("Email");

  private final TextField fullName = new TextField("Full Name");

  private final TextField username = new TextField("Username");

  private final Button registerButton = new Button("Register");

  private final Div notificationLayout = new Div();

  private final H2 titleSpan = new H2("Add your details");

  private final Div description = new Div();

  private final Binder<UserRegistrationOrcIdInformation> registrationInformationBinder = new Binder<>(
      UserRegistrationOrcIdInformation.class);

  private final transient UserInformationService userInformationService;

  public UserRegistrationOrcIdComponent(UserInformationService userInformationService) {
    this.userInformationService = Objects.requireNonNull(userInformationService);
    registerButton.addClassName("primary");
    username.setHelperText("Your unique user name, visible to other users");
    description.add(
        "Please complete missing information to create an account with us.");
    add(titleSpan, notificationLayout, description, fullName, email, username, registerButton);
    setFieldValidation();
    addRegistrationButtonListener();
  }

  private void setFieldValidation() {
    registrationInformationBinder.forField(fullName)
        .asRequired("Please provide a full name")
        .withValidator(new RegexpValidator("Please provide a valid full name", "\\S.*"))
        .bind(UserRegistrationOrcIdInformation::fullName,
            UserRegistrationOrcIdInformation::setFullName);
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
        .bind(UserRegistrationOrcIdInformation::email, UserRegistrationOrcIdInformation::setEmail);
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
        .bind(UserRegistrationOrcIdInformation::userName,
            UserRegistrationOrcIdInformation::setUserName);
  }


  private void addRegistrationButtonListener() {
    registerButton.addClickShortcut(Key.ENTER);
    registerButton.addClickListener(event -> {
      registrationInformationBinder.validate();
      if (registrationInformationBinder.isValid()) {
        clearNotifications();
        UserRegistrationOrcIdInformation userRegistrationOrcIdInformation = new UserRegistrationOrcIdInformation(
            fullName.getValue().strip(), email.getValue().strip(), username.getValue().strip());
        fireEvent(
            new UserRegistrationOrcIdEvent(this, event.isFromClient(),
                userRegistrationOrcIdInformation));
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

  public void setEmail(String email) {
    this.email.setValue(email);
  }

  public void setUsername(String username) {
    this.username.setValue(username);
  }

  public void setFullName(String fullName) {
    this.fullName.setValue(fullName);
  }

  public void addRegistrationListener(ComponentEventListener<UserRegistrationOrcIdEvent> listener) {
    addListener(UserRegistrationOrcIdEvent.class, listener);
  }

  public static final class UserRegistrationOrcIdInformation implements Serializable {

    @Serial
    private static final long serialVersionUID = -5227813990533884919L;
    @NotEmpty
    private String fullName = "";
    @NotEmpty
    private String email = "";
    @NotEmpty
    private String userName = "";

    public UserRegistrationOrcIdInformation(String fullName, String email, String userName) {
      this.fullName = fullName;
      this.email = email;
      this.userName = userName;
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

    public String fullName() {
      return fullName;
    }

    public String email() {
      return email;
    }

    public String userName() {
      return userName;
    }

  }

  public static class UserRegistrationOrcIdEvent extends
      ComponentEvent<UserRegistrationOrcIdComponent> {

    private final UserRegistrationOrcIdInformation userRegistrationOrcIdInformation;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public UserRegistrationOrcIdEvent(UserRegistrationOrcIdComponent source, boolean fromClient,
        UserRegistrationOrcIdInformation userRegistrationOrcIdInformation) {
      super(source, fromClient);
      this.userRegistrationOrcIdInformation = userRegistrationOrcIdInformation;
    }

    public UserRegistrationOrcIdInformation userRegistrationOrcIdInformation() {
      return userRegistrationOrcIdInformation;
    }
  }
}
