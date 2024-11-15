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
import life.qbic.identity.domain.registry.DomainRegistry;
import life.qbic.identity.domain.repository.UserRepository;

/**
 * <b>User Registration Service</b>
 *
 * <p>Application service that can be used to register users for the user management domain
 * context.
 *
 * @since 1.0.0
 */
public final class IdentityService {

  private final UserRepository userRepository;

  public IdentityService(UserRepository userRepository) {
    super();
    this.userRepository = userRepository;
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
   * @param userEmailAddress the user's email address for whom the password reset shall be issued
   * @return application response with success or failure information
   * @since 1.0.0
   */
  public ApplicationResponse requestPasswordReset(String userEmailAddress) {
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.from(userEmailAddress);
    } catch (EmailValidationException e) {
      return ApplicationResponse.failureResponse(e);
    }
    // fetch user
    var optionalUser = userRepository.findByEmail(emailAddress);
    if (optionalUser.isEmpty()) {
      return ApplicationResponse.failureResponse(new UserNotFoundException("User not found"));
    }

    // get user
    var user = optionalUser.get();

    // We only allow password reset for users with confirmed email address
    if (!user.isActive()) {
      return ApplicationResponse.failureResponse(new UserNotActivatedException("User not active"));
    }

    user.resetPassword();
    return ApplicationResponse.successResponse();
  }

  public ApplicationResponse requestUserNameChange(String userId, String userName) {
    if (isNull(userName) || userName.isBlank()) {
      return ApplicationResponse.failureResponse(new EmptyUserNameException());
    }
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
    if (userRepository.findByUserName(userName).isPresent()) {
      return ApplicationResponse.failureResponse(new UserNameNotAvailableException());
    }
    user.setNewUserName(userName);
    userRepository.updateUser(user);
    return ApplicationResponse.successResponse();
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
            .onValue(userRepository::updateUser));
  }

  private Result<EncryptedPassword, RuntimeException> attemptPasswordEncryption(
      char[] newPassword) {
    try {
      return Result.fromValue(EncryptedPassword.from(newPassword));
    } catch (PasswordValidationException e) {
      return Result.fromError(e);
    }
  }

  public static class EmptyUserNameException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -150902871229730428L;

    public EmptyUserNameException() {
      super();
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
