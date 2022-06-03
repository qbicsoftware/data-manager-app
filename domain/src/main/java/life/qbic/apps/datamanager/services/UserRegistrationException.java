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

  public Optional<Exception> emailFormatException(){
   return Optional.empty();
  }

  public Optional<Exception> fullNameException(){
    return Optional.empty();
  }

  public Optional<Exception> passwordException(){
    return Optional.empty();
  }

}
