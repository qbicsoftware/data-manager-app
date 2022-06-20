package life.qbic.identityaccess.application;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public abstract class ApplicationException extends RuntimeException {

  protected ApplicationException() {
    super();
  }

  protected ApplicationException(String message) {
    super(message);
  }

}
