package life.qbic.identity.application.user;

import static java.util.Objects.isNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.application.commons.Result;
import life.qbic.identity.application.ServiceException;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.EmailAddress.EmailValidationException;
import life.qbic.identity.domain.model.EncryptedPassword;
import life.qbic.identity.domain.model.EncryptedPassword.PasswordValidationException;
import life.qbic.identity.domain.model.FullName;
import life.qbic.identity.domain.model.FullName.FullNameValidationException;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.model.UserNamePolicy;
import life.qbic.identity.domain.registry.DomainRegistry;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>User Registration Service</b>
 *
 * <p>Application service that can be used to register users for the user management domain
 * context.
 *
 * @since 1.0.0
 */
public final class IdentityService {

  private static final Logger log = LoggerFactory.logger(IdentityService.class);

  private final UserRepository userRepository;

  /**
   * Optional: when present, a completed password change invalidates every other pending reset token
   * for that account (AUTH-R-05, story #1674).
   *
   * <p>Optional rather than required so the domain-focused unit tests and the ORCID/registration
   * paths can construct the service without a token store. When it is absent, no token can have been
   * issued through this service anyway.</p>
   */
  private final java.util.function.Consumer<String> invalidationAfterPasswordChange;

  public IdentityService(UserRepository userRepository) {
    this(userRepository, userId -> {
    });
  }

  public IdentityService(UserRepository userRepository,
      java.util.function.Consumer<String> invalidationAfterPasswordChange) {
    super();
    this.userRepository = java.util.Objects.requireNonNull(userRepository,
        "userRepository must not be null");
    this.invalidationAfterPasswordChange = java.util.Objects.requireNonNull(
        invalidationAfterPasswordChange, "invalidationAfterPasswordChange must not be null");
  }

  /**
   * Registers a user in the user management domain.
   *
   * <p>Note: the raw password will be overwritten before the method returns. This is only
   * guaranteed though, when the method returns without any exceptions. In the later case, it is the
   * client's responsibility to handle the raw password.
   *
   * @param fullName    the full name of the user
   * @param userName
   * @param email       the mail address of the user
   * @param rawPassword the raw password provided by the user
   * @return a registration response with information about if the registration was successful or
   * not.
   * @since 1.0.0
   */
  public ApplicationResponse registerUser(final String fullName, String userName, final String email,
      final char[] rawPassword) {

    var registrationResponse = validateInput(fullName, userName, email, rawPassword);
    if (registrationResponse.hasFailures()) {
      return registrationResponse;
    }

    userName = normalizeUserName(userName);

    var userDomainService = DomainRegistry.instance().userDomainService();
    if (userDomainService.isEmpty()) {
      throw new ApplicationException("User registration failed.");
    }

    var userEmail = EmailAddress.from(email);
    var userFullName = FullName.from(fullName);
    var userPassword = EncryptedPassword.from(rawPassword);

    if (userRepository.findByEmail(userEmail).isPresent()) {
      return ApplicationResponse.failureResponse(new UserExistsException());
    }

    if (userRepository.findByUserName(userName).isPresent()) {
      return ApplicationResponse.failureResponse(new UserNameNotAvailableException());
    }

    // Trigger the user creation in the domain service
    userDomainService.get().createUser(userFullName, userName, userEmail, userPassword);

    // Overwrite the password
    Arrays.fill(rawPassword, '-');

    return ApplicationResponse.successResponse();
  }

  public ApplicationResponse registerOpenIdUser(String fullName, String userName, String email,
      String oidcIssuer, String oidcId) {

    var validationResponse = validateInputOidcInput(fullName, userName, email, oidcIssuer, oidcId);
    if (validationResponse.hasFailures()) {
      return validationResponse;
    }

    userName = normalizeUserName(userName);

    var userDomainService = DomainRegistry.instance().userDomainService();
    if (userDomainService.isEmpty()) {
      throw new ApplicationException("User registration failed.");
    }

    var userEmail = EmailAddress.from(email);
    var userFullName = FullName.from(fullName);

    if (userRepository.findByEmail(userEmail).isPresent()) {
      return ApplicationResponse.failureResponse(new UserExistsException());
    }

    if (userRepository.findByUserName(userName).isPresent()) {
      return ApplicationResponse.failureResponse(new UserNameNotAvailableException());
    }

    // Trigger the user creation in the domain service
    userDomainService.get().createOidcUser(userFullName, userName, userEmail, oidcIssuer, oidcId);

    return ApplicationResponse.successResponse();
  }

  private ApplicationResponse validateInput(String fullName, String userName, String email,
      char[] rawPassword) {
    List<RuntimeException> failures = new ArrayList<>();

    try {
      EmailAddress.from(email);
    } catch (EmailValidationException e) {
      failures.add(e);
    }
    try {
      FullName.from(fullName);
    } catch (FullNameValidationException e) {
      failures.add(e);
    }
    if (isNull(userName) || userName.isBlank()) {
      failures.add(new EmptyUserNameException());
    } else if (userName.strip().length() > UserNamePolicy.maxLength()) {
      failures.add(new UserNameTooLongException());
    }
    try {
      EncryptedPassword.from(rawPassword);
    } catch (PasswordValidationException e) {
      failures.add(e);
    }

    if (failures.isEmpty()) {
      return ApplicationResponse.successResponse();
    }

    return ApplicationResponse.failureResponse(failures.toArray(RuntimeException[]::new));
  }

  private ApplicationResponse validateInputOidcInput(String fullName, String userName, String email,
      String oidcIssuer, String oidcId) {
    List<RuntimeException> failures = new ArrayList<>();

    try {
      EmailAddress.from(email);
    } catch (EmailValidationException e) {
      failures.add(e);
    }
    try {
      FullName.from(fullName);
    } catch (FullNameValidationException e) {
      failures.add(e);
    }
    if (isNull(userName) || userName.isBlank()) {
      failures.add(new EmptyUserNameException());
    } else if (userName.strip().length() > UserNamePolicy.maxLength()) {
      failures.add(new UserNameTooLongException());
    }
    if (isNull(oidcIssuer) || oidcIssuer.isBlank()) {
      failures.add(new EmptyOidcIssuerException());
    }
    if (isNull(oidcId) || oidcId.isBlank()) {
      failures.add(new EmptyOidcIdException());
    }

    if (failures.isEmpty()) {
      return ApplicationResponse.successResponse();
    }

    return ApplicationResponse.failureResponse(failures.toArray(RuntimeException[]::new));
  }

  /**
   * Requests a password reset for a user.
   *
   * <p><b>The outcome does not describe the account.</b> An unknown address, an unconfirmed account
   * and a confirmed account all return {@link ApplicationResponse#successResponse()}, and the
   * notification is dispatched only for the last of them. AUTH-NFR-03 forbids telling an
   * unauthenticated caller whether an account exists or has been confirmed, and this method used to
   * answer both questions precisely: {@code UserNotFoundException("User not found")} and
   * {@code UserNotActivatedException("User not active")} were distinct, structured failures that
   * the caller rendered verbatim.</p>
   *
   * <p>Only a malformed address is reported, because that is a property of the input rather than of
   * any account — a visitor who mistyped their own address must be told, and nobody learns anything
   * about another account by learning that a string is not an email address.</p>
   *
   * <p>The caller must therefore render one fixed message for every successful call. If a caller
   * branches on "was a reset actually requested", the uniform response is pointless.</p>
   *
   * @param userEmailAddress the user's email address for whom the password reset shall be issued
   * @return success for any well-formed address; failure only for a malformed one
   * @since 1.0.0
   */
  public ApplicationResponse requestPasswordReset(String userEmailAddress) {
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.from(userEmailAddress);
    } catch (EmailValidationException e) {
      return ApplicationResponse.failureResponse(e);
    }

    var optionalUser = userRepository.findByEmail(emailAddress);
    if (optionalUser.isEmpty()) {
      // Uniform outcome. Logged at debug and without the address, so the log does not become the
      // oracle the response no longer is.
      log.debug("Password reset requested for an address with no account");
      return ApplicationResponse.successResponse();
    }

    var user = optionalUser.get();
    if (!user.isActive()) {
      // A reset link is only meaningful once the address is confirmed (the token flow confirms and
      // sets the password in one step, see AUTH-R-07), so no notification is dispatched. The caller
      // is told nothing different, and neither is the address owner — sending mail to an
      // unconfirmed address would be mail to a mailbox nobody has proven they control.
      log.debug("Password reset requested for an unconfirmed account");
      return ApplicationResponse.successResponse();
    }

    user.resetPassword();
    return ApplicationResponse.successResponse();
  }

  public ApplicationResponse requestUserNameChange(String userId, String userName) {
    if (isNull(userName) || userName.isBlank()) {
      return ApplicationResponse.failureResponse(new EmptyUserNameException());
    }
    userName = normalizeUserName(userName);
    UserId id = UserId.from(userId);
    var optionalUser = userRepository.findById(id);
    if (optionalUser.isEmpty()) {
      return ApplicationResponse.failureResponse(new UserNotFoundException("User not found"));
    }
    // get user
    var user = optionalUser.get();
    if (user.userName().equals(userName)) {
      return ApplicationResponse.successResponse();
    }
    // Grandfather clause: the length limit applies to the value the user is switching to, not to
    // the value already stored. A user whose persisted username predates the limit may keep it
    // (and re-submit it unchanged) but must pick a compliant one when changing it.
    if (userName.length() > UserNamePolicy.maxLength()) {
      return ApplicationResponse.failureResponse(new UserNameTooLongException());
    }
    if (userRepository.findByUserName(userName).isPresent()) {
      return ApplicationResponse.failureResponse(new UserNameNotAvailableException());
    }
    user.setNewUserName(userName);
    userRepository.updateUser(user);
    return ApplicationResponse.successResponse();
  }

  /**
   * Normalizes a username before it is persisted or looked up, so that surrounding whitespace
   * cannot create distinct usernames or cause spurious availability conflicts.
   */
  private static String normalizeUserName(String userName) {
    return isNull(userName) ? null : userName.strip();
  }

  /**
   * Sets a new password for a given user.
   * <p>
   * Success or failures of the request need to be evaluated by the client via the
   * {@link ApplicationResponse}.
   *
   * @param userId         the user's id for whom the new password shall be set
   * @param newRawPassword the user's request new password
   * @return an application response. In the case of a password validation failure, the
   * {@link ApplicationResponse#failures()} will contain an exception with type
   * {@link PasswordValidationException}.
   * @since 1.0.0
   */
  public Result<EncryptedPassword, RuntimeException> newUserPassword(String userId,
      char[] newRawPassword) {
    Result<User, ServiceException> user = Result.<UserId, ServiceException>fromValue(
            UserId.from(userId))
        .map(userRepository::findById)
        .flatMap(it -> it.<Result<User, ServiceException>>map(Result::fromValue)
            .orElseGet(() -> Result.fromError(new ServiceException("Unknown user id"))));
    if (user.isError()) {
      return Result.fromError(user.getError());
    }

    return Result
        .<char[], RuntimeException>fromValue(newRawPassword)
        .flatMap(this::attemptPasswordEncryption)
        .onValue(password -> user
            .onValue(u -> u.setNewPassword(password))
            .onValue(userRepository::updateUser))
        // A completed password change kills every outstanding reset link for the account. Without
        // this, a user who requested two resets and used the second leaves the first still valid —
        // and an attacker holding that older link can still take the account over.
        .onValue(password -> user.onValue(u -> invalidationAfterPasswordChange.accept(
            u.id().get())));
  }

  private Result<EncryptedPassword, RuntimeException> attemptPasswordEncryption(
      char[] newPassword) {
    try {
      return Result.fromValue(EncryptedPassword.from(newPassword));
    } catch (PasswordValidationException e) {
      return Result.fromError(e);
    }
  }

  public void setOidc(String userId, String oidcId, String oidcIssuer) throws IssueOidcException {
    var alreadyConnectedUser = userRepository.findByOidc(oidcId, oidcIssuer);
    if (alreadyConnectedUser.isPresent()) {
      throw new IssueOidcException("OpenID already in use.");
    }
    var user = userRepository.findById(UserId.from(userId)).orElseThrow(() -> new IssueOidcException("User not found"));
    user.setOidcEntry(oidcIssuer, oidcId);
    userRepository.updateUser(user);
  }

  public static class IssueOidcException extends RuntimeException {
    public IssueOidcException(String message) {
      super(message);
    }
  }

  public static class EmptyUserNameException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -150902871229730428L;

    public EmptyUserNameException() {
      super();
    }
  }

  /**
   * Indicates that a provided username exceeds {@link UserNamePolicy#maxLength()} characters.
   *
   * @since 1.19.0
   */
  public static class UserNameTooLongException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 7734019832145098761L;

    public UserNameTooLongException() {
      super("Username must not exceed " + UserNamePolicy.maxLength() + " characters.");
    }
  }

  public static class UserExistsException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 3147229431249844901L;

    public UserExistsException() {
      super();
    }
  }

  public static class UserNameNotAvailableException extends ApplicationException {


    @Serial
    private static final long serialVersionUID = 4409722243047442583L;

    public UserNameNotAvailableException() {
      super();
    }
  }

  /**
   * Activates a user with the userId provided. If no user is matched then this method does
   * nothing.
   *
   * @param userId the id of the user to be activated
   * @throws UserNotFoundException when no user with the provided user id can be found.
   * @since 1.0.0
   */
  public void confirmUserEmail(String userId) throws UserNotFoundException {
    Optional<User> optionalUser = userRepository.findById(UserId.from(userId));
    optionalUser.ifPresentOrElse(user -> {
      user.confirmEmail();
      userRepository.updateUser(user);
    }, () -> {
      throw new UserNotFoundException("Unknown user. Could not confirm the email address.");
    });
  }

  /**
   * <p>
   * An exception to be thrown if a user is not activated. This implies that the user cannot log in
   * to the application
   * </p>
   *
   * <p><b>Not a password-reset failure any more.</b> {@link #requestPasswordReset(String)} no longer
   * raises it: reporting "user not active" from an unauthenticated form disclosed that an address is
   * registered but unconfirmed, which is more useful to an attacker than plain existence. The type is
   * still used where activation genuinely gates an authenticated operation.</p>
   */
  public static class UserNotActivatedException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -4253849498611530692L;

    UserNotActivatedException(String message) {
      super(message);
    }
  }

  public static class EmptyOidcIssuerException extends RuntimeException {

  }

  public static class EmptyOidcIdException extends RuntimeException {

  }
}
