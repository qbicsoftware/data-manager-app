package life.qbic.apps.datamanager.services;

import java.io.Serial;
import java.util.Optional;
import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.domain.user.EmailAddress.EmailValidationException;
import life.qbic.domain.user.EncryptedPassword.PasswordValidationException;
import life.qbic.domain.user.FullName.InvalidFullNameException;

/**
 * <h1>Exception that indicates violations during the user registration process</h1>
 *
 * <p>This exception is supposed to be thrown, if the provided user credentials violate one or more policies
 *  during the registration process. It's intention is to contain the exceptions thrown for each of the vioolated credentials</p>
 *
 *  Example: A user provides a malformed email address and an empty user name.
 *  Since this violates the established policies, the method will catch the individual ApplicationExceptions and add them to this Exception
 *
 * @since 1.0.0
 */
public class UserRegistrationException extends ApplicationException {

  @Serial
  private static final long serialVersionUID = 1026978635211901782L;
  private final transient EmailValidationException emailFormatException;
  private final transient PasswordValidationException invalidPasswordException;
  private final transient InvalidFullNameException fullNameException;
  private final transient RuntimeException unexpectedException;

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private EmailValidationException emailFormatException;

    private PasswordValidationException invalidPasswordException;

    private InvalidFullNameException fullNameException;

    private RuntimeException unexpectedException;

    protected Builder() {

    }

    public Builder withEmailFormatException(EmailValidationException e) {
      emailFormatException = e;
      return this;
    }

    public Builder withFullNameException(InvalidFullNameException e) {
      fullNameException = e;
      return this;
    }

    public Builder withInvalidPasswordException(PasswordValidationException e) {
      invalidPasswordException = e;
      return this;
    }

    public Builder withUnexpectedException(RuntimeException e) {
      unexpectedException = e;
      return this;
    }

    public UserRegistrationException build() {
      return new UserRegistrationException(this);
    }
  }

  private UserRegistrationException(Builder builder) {
    emailFormatException = builder.emailFormatException;
    fullNameException = builder.fullNameException;
    invalidPasswordException = builder.invalidPasswordException;
    unexpectedException = builder.unexpectedException;
  }

  public Optional<EmailValidationException> emailFormatException() {
    return Optional.ofNullable(emailFormatException);
  }

  public Optional<InvalidFullNameException> fullNameException() {
    return Optional.ofNullable(fullNameException);
  }

  public Optional<PasswordValidationException> passwordException() {
    return Optional.ofNullable(invalidPasswordException);
  }

  public Optional<RuntimeException> unexpectedException() {
    return Optional.ofNullable(unexpectedException);
  }

}
