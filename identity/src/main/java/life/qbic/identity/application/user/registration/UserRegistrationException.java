package life.qbic.identity.application.user.registration;

import java.io.Serial;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserExistsException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.identity.domain.model.EmailAddress.EmailValidationException;
import life.qbic.identity.domain.model.EncryptedPassword.PasswordValidationException;
import life.qbic.identity.domain.model.FullName.FullNameValidationException;

/**
 * <h1>Exception that indicates violations during the user registration process</h1>
 *
 * <p>This exception is supposed to be thrown, if the provided user credentials violate one or more
 * policies
 * during the registration process. It's intention is to contain the exceptions thrown for each of
 * the vioolated credentials</p>
 * <p>
 * Example: A user provides a malformed mail value and an empty user name. Since this violates the
 * established policies, the method will catch the individual ApplicationExceptions and add them to
 * this Exception
 *
 * @since 1.0.0
 */
public class UserRegistrationException extends ApplicationException {

  @Serial
  private static final long serialVersionUID = 1026978635211901782L;
  private final transient EmailValidationException emailFormatException;
  private final transient PasswordValidationException invalidPasswordException;
  private final transient FullNameValidationException fullNameException;

  private final transient UserNameNotAvailableException userNameNotAvailableException;
  private final transient EmptyUserNameException emptyUserNameException;

  private final transient UserExistsException userExistsException;
  private final transient RuntimeException unexpectedException;

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private EmailValidationException emailFormatException;

    private PasswordValidationException invalidPasswordException;

    private FullNameValidationException fullNameException;

    private UserExistsException userExistsException;
    private RuntimeException unexpectedException;
    private UserNameNotAvailableException userNameNotAvailableException;
    private EmptyUserNameException emptyUserNameException;

    protected Builder() {

    }

    public Builder withEmailFormatException(EmailValidationException e) {
      emailFormatException = e;
      return this;
    }

    public Builder withFullNameException(FullNameValidationException e) {
      fullNameException = e;
      return this;
    }

    public Builder withInvalidPasswordException(PasswordValidationException e) {
      invalidPasswordException = e;
      return this;
    }

    public Builder withUserExistsException(UserExistsException e) {
      userExistsException = e;
      return this;
    }

    public Builder withUnexpectedException(RuntimeException e) {
      unexpectedException = e;
      return this;
    }

    public UserRegistrationException build() {
      return new UserRegistrationException(this);
    }

    public Builder withUserNameNotAvailableException(UserNameNotAvailableException e) {
      this.userNameNotAvailableException = e;
      return this;
    }

    public Builder withEmptyUserNameException(EmptyUserNameException emptyUserNameException) {
      this.emptyUserNameException = emptyUserNameException;
      return this;
    }
  }

  private UserRegistrationException(Builder builder) {
    emailFormatException = builder.emailFormatException;
    fullNameException = builder.fullNameException;
    invalidPasswordException = builder.invalidPasswordException;
    userExistsException = builder.userExistsException;
    unexpectedException = builder.unexpectedException;
    userNameNotAvailableException = builder.userNameNotAvailableException;
    emptyUserNameException = builder.emptyUserNameException;
  }

  public Optional<EmailValidationException> emailFormatException() {
    return Optional.ofNullable(emailFormatException);
  }

  public Optional<FullNameValidationException> fullNameException() {
    return Optional.ofNullable(fullNameException);
  }

  public Optional<PasswordValidationException> passwordException() {
    return Optional.ofNullable(invalidPasswordException);
  }

  public Optional<UserExistsException> userExistsException() {
    return Optional.ofNullable(userExistsException);
  }

  public Optional<UserNameNotAvailableException> userNameNotAvailableException() {
    return Optional.ofNullable(userNameNotAvailableException);
  }

  public Optional<EmptyUserNameException> emptyUserNameException() {
    return Optional.ofNullable(emptyUserNameException);
  }

  public Optional<RuntimeException> unexpectedException() {
    return Optional.ofNullable(unexpectedException);
  }

}
