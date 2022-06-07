package life.qbic.apps.datamanager.services;

import java.io.Serial;
import java.util.Optional;
import life.qbic.apps.datamanager.ApplicationException;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserRegistrationException extends ApplicationException {

  @Serial
  private static final long serialVersionUID = 1026978635211901782L;
  private final transient ApplicationException emailFormatException;
  private final transient ApplicationException invalidPasswordException;
  private final transient ApplicationException fullNameException;
  private final transient RuntimeException unexpectedException;

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private ApplicationException emailFormatException;

    private ApplicationException invalidPasswordException;

    private ApplicationException fullNameException;

    private RuntimeException unexpectedException;

    protected Builder() {

    }

    public Builder withEmailFormatException(ApplicationException e) {
      emailFormatException = e;
      return this;
    }

    public Builder withFullNameException(ApplicationException e) {
      fullNameException = e;
      return this;
    }

    public Builder withInvalidPasswordException(ApplicationException e) {
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

  public Optional<ApplicationException> emailFormatException() {
    return Optional.ofNullable(emailFormatException);
  }

  public Optional<ApplicationException> fullNameException() {
    return Optional.ofNullable(fullNameException);
  }

  public Optional<ApplicationException> passwordException() {
    return Optional.ofNullable(invalidPasswordException);
  }

  public Optional<RuntimeException> unexpectedException() {
    return Optional.ofNullable(unexpectedException);
  }

}
