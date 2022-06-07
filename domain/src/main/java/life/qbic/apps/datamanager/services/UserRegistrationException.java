package life.qbic.apps.datamanager.services;

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
  private final Optional<Exception> emailFormatException;
  private final Optional<Exception> invalidPasswordException;
  private final Optional<Exception> fullNameException;
  private final Optional<Exception> unexpectedException;

  public static Builder builder() {
    return new Builder();
  }
  public static class Builder {
    private Exception emailFormatException;

    private Exception invalidPasswordException;

    private Exception fullNameException;

    private Exception unexpectedException;
    protected Builder() {

    }

    public Builder withEmailFormatException(Exception e) {
      emailFormatException = e;
      return this;
    }

    public Builder withFullNameException(Exception e) {
      fullNameException = e;
      return this;
    }

    public Builder withInvalidPasswordException(Exception e) {
      invalidPasswordException = e;
      return this;
    }

    public Builder withUnexpectedException(Exception e) {
      unexpectedException = e;
      return this;
    }

    public UserRegistrationException build() {
      return new UserRegistrationException(this);
    }
  }

  private UserRegistrationException(Builder builder) {
    emailFormatException = Optional.ofNullable(builder.emailFormatException);
    fullNameException = Optional.ofNullable(builder.fullNameException);
    invalidPasswordException = Optional.ofNullable(builder.invalidPasswordException);
    unexpectedException = Optional.ofNullable(builder.unexpectedException);
  }

  public Optional<Exception> emailFormatException(){
   return Optional.empty();
  }

  public Optional<Exception> fullNameException(){
    return Optional.empty();
  }

  public Optional<Exception> passwordException(){
    return Optional.empty();
  }

  public Optional<Exception> unexpectedException() {return Optional.empty();}

}
