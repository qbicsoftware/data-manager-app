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
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.identity.api.Passwords;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserNames;

/**
 * <b>The one registration form: local and ORCID registration render the same component.</b>
 *
 * <p>This class used to exist twice — {@code UserRegistrationComponent} and
 * {@code UserRegistrationOrcIdComponent} — with roughly 80 % identical content: the same binder chain,
 * the same field layout, the same notification rendering and two parallel {@code …Event} /
 * {@code …Information} inner classes. The only real difference is that the ORCID flow does not collect a
 * password (ORCID already authenticated the visitor) and prefills the fields from the OIDC claims.</p>
 *
 * <p>That duplication was not cosmetic. A rule added to one form did not reach the other, and the two
 * forms had to be kept in step by hand for every change to the email or user-name rule. The account
 * oracle that {@code AUTH-NFR-03} removed lived in <em>both</em> email validators, and it could come back
 * in either one. So the difference is now a {@link Mode} parameter and everything else is one code path:
 * same fields, same validator chain, same messages, same failure rendering.</p>
 *
 * <p>Not a Spring bean on purpose. Each registration route constructs its own form with an explicit
 * {@link Mode}; as a {@code @UIScope} bean an additional, unused instance was created per UI and the
 * mode-dependent construction had no way to be expressed.</p>
 *
 * @see UserRegistrationMain
 * @see RegistrationOrcIdMain
 */
public class UserRegistrationComponent extends Div {

  @Serial
  private static final long serialVersionUID = -1189104139053489520L;

  /**
   * <b>How the visitor arrived, and therefore what the form asks.</b>
   *
   * <p>The only differences between the two registration flows are collected here, so that the rest of
   * the component cannot fork: a title, an explanatory line, and whether a password is collected (and
   * therefore whether a password can be reset).</p>
   */
  public enum Mode {

    /**
     * The visitor registered on the platform: an email address, a user name and a password are needed.
     */
    LOCAL("Register", null, true),

    /**
     * The visitor authenticated with ORCID and completes the account: no password is collected, because
     * the passwordless ORCID account is what {@code AUTH-R-07} later fixes through an invitation. The
     * fields are prefilled from the OIDC claims in {@link RegistrationOrcIdMain#beforeEnter}.
     */
    ORCID("Add your details",
        "Please complete missing information to create an account with us.", false);

    private final String title;
    private final String description;
    private final boolean collectsPassword;

    Mode(String title, String description, boolean collectsPassword) {
      this.title = title;
      this.description = description;
      this.collectsPassword = collectsPassword;
    }

    public String title() {
      return title;
    }

    /**
     * @return the explanatory line above the fields, or {@code null} when the title says enough
     */
    public String description() {
      return description;
    }

    /**
     * @return whether this flow asks for, and validates, a password
     */
    public boolean collectsPassword() {
      return collectsPassword;
    }
  }

  /*Validation via Binder does not show error message if an email field is used, see
   * https://github.com/vaadin/flow-components/issues/4618 for details */
  private final TextField email = new TextField("Email");

  private final PasswordField password = new PasswordField("Password");

  private final TextField fullName = new TextField("Full Name");

  private final TextField username = new TextField("Username");

  private final Button registerButton = new Button("Register");

  private final Div notificationLayout = new Div();

  private final H2 titleSpan;

  private final Binder<UserRegistrationInformation> registrationInformationBinder = new Binder<>(
      UserRegistrationInformation.class);

  private final transient UserInformationService userInformationService;

  private final Mode mode;

  public UserRegistrationComponent(UserInformationService userInformationService, Mode mode) {
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.mode = Objects.requireNonNull(mode, "mode must not be null");
    this.titleSpan = new H2(mode.title());
    addClassName("user-registration-component");
    addClassName("card-layout");
    registerButton.addClassName("primary");
    username.setMaxLength(UserNames.MAX_LENGTH);
    username.setHelperText("Your unique user name, visible to other users (max. "
        + UserNames.MAX_LENGTH + " characters)");
    add(titleSpan, notificationLayout);
    if (mode.description() != null) {
      Div description = new Div();
      description.add(mode.description());
      add(description);
    }
    add(fullName, email, username);
    if (mode.collectsPassword()) {
      password.setHelperText(Passwords.hint());
      add(password);
    }
    add(registerButton);
    if (mode.collectsPassword()) {
      // Only a flow that collects a password can offer to reset one. On the ORCID form the visitor has
      // no password yet, so "Forgot your password?" would point at a flow that cannot help them.
      addRoutingLinks();
    }
    setFieldValidation();
    addRegistrationButtonListener();
  }

  private void addRoutingLinks() {
    RouterLink resetLink = new RouterLink("RESET", ResetPasswordMain.class);
    Span resetSpan = new Span(new Text("Forgot your password? "), resetLink);
    add(resetSpan);
  }

  /**
   * Binds every field this mode asks for, with one validator chain shared by both modes.
   *
   * <p>Only the password binding is conditional, because only the local flow has that field. The email,
   * user name and full name rules are deliberately written once — that is the point of merging the two
   * forms.</p>
   */
  private void setFieldValidation() {
    registrationInformationBinder.forField(fullName)
        .asRequired("Please provide a full name")
        .withValidator(new RegexpValidator("Please provide a valid full name", "\\S.*"))
        .bind(UserRegistrationInformation::fullName, UserRegistrationInformation::setFullName);
    registrationInformationBinder.forField(email)
        .asRequired("Please provide an email address")
        .withValidator(new EmailValidator("Please provide a valid email address"))
        // No account-existence check here, deliberately (AUTH-NFR-03, FEAT-AUTH-FLOWS-03).
        // This validator used to call userInformationService.isEmailAvailable() and answer
        // "Email address already in use", which is a repeatable, unauthenticated oracle for "does
        // this address have an account?". Removed here AND in the submit path (RegistrationSubmission);
        // fixing only one of the two would just move the disclosure one step later.
        //
        // Duplicate registrations are now handled uniformly: the visitor sees the same outcome as a
        // successful registration, and the address owner is notified by email instead.
        //
        // This comment is the only place the rule is stated, because there is now only one email
        // validator. The ORCID form inherits it by being the same component.
        .bind(UserRegistrationInformation::email, UserRegistrationInformation::setEmail);
    registrationInformationBinder.forField(username)
        .asRequired("Please provide an user name")
        .withValidator(new RegexpValidator("Please provide a valid user name", ".*\\S+.*"))
        .withValidator((Validator<String>) (value, context) -> {
          if (UserNames.exceedsMaxLength(value)) {
            return ValidationResult.error(
                "User name must not exceed " + UserNames.MAX_LENGTH + " characters");
          }
          if (userInformationService.isUserNameAvailable(value)) {
            return ValidationResult.ok();
          } else {
            return ValidationResult.error("User Name is already in use");
          }
        })
        .bind(UserRegistrationInformation::userName, UserRegistrationInformation::setUserName);
    if (mode.collectsPassword()) {
      registrationInformationBinder.forField(password)
          .asRequired("Please provide a password")
          .withValidator(Passwords::satisfiesMinLength, Passwords.tooShortMessage())
          .bind(UserRegistrationInformation::password, UserRegistrationInformation::setPassword);
    }
  }

  private void addRegistrationButtonListener() {
    registerButton.addClickShortcut(Key.ENTER);
    registerButton.addClickListener(event -> {
      registrationInformationBinder.validate();
      if (registrationInformationBinder.isValid()) {
        clearNotifications();
        fireEvent(new UserRegistrationEvent(this, event.isFromClient(), currentInput()));
      }
    });
  }

  /**
   * Reads the form into the value the submission consumes.
   *
   * <p>Package-private so that a spec can assert what the form would submit without simulating a browser
   * click — which is also how the "the ORCID form submits no password" rule is pinned.</p>
   *
   * @return what the visitor entered, with an empty password when this mode does not collect one
   */
  UserRegistrationInformation currentInput() {
    return new UserRegistrationInformation(
        fullName.getValue().strip(),
        email.getValue().strip(),
        username.getValue().strip(),
        mode.collectsPassword() ? password.getValue().strip() : "");
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

  /**
   * Prefills the email address, used by the ORCID flow for the {@code email} claim.
   *
   * @param email the address from the OIDC assertion
   */
  public void setEmail(String email) {
    this.email.setValue(email);
  }

  /**
   * Prefills the user name, used by the ORCID flow for the {@code preferred_username} claim.
   *
   * @param username the proposed user name from the OIDC assertion
   */
  public void setUsername(String username) {
    this.username.setValue(username);
  }

  /**
   * Prefills the full name, used by the ORCID flow for the name claims.
   *
   * @param fullName the name assembled from the OIDC assertion
   */
  public void setFullName(String fullName) {
    this.fullName.setValue(fullName);
  }

  /**
   * <b>What the form collected, independent of which mode collected it.</b>
   *
   * <p>{@code password} is empty on the ORCID flow, which does not ask for one — see {@code AUTH-R-07},
   * which will give those accounts a password through a separate invitation.</p>
   */
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

    @Serial
    private static final long serialVersionUID = 4478331034446132091L;

    private final UserRegistrationInformation userRegistrationInformation;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     * @param userRegistrationInformation what the visitor entered
     */
    public UserRegistrationEvent(UserRegistrationComponent source, boolean fromClient,
        UserRegistrationInformation userRegistrationInformation) {
      super(source, fromClient);
      this.userRegistrationInformation = userRegistrationInformation;
    }

    public UserRegistrationInformation information() {
      return userRegistrationInformation;
    }
  }
}
