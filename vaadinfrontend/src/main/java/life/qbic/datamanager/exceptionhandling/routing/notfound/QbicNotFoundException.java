package life.qbic.datamanager.exceptionhandling.routing.notfound;

/**
 * Is needed as {@link com.vaadin.flow.router.NotFoundException} does not provide a constructor with
 * cause.
 */
public class QbicNotFoundException extends RuntimeException {

  public QbicNotFoundException() {
  }

  public QbicNotFoundException(String message) {
    super(message);
  }

  public QbicNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public QbicNotFoundException(Throwable cause) {
    super(cause);
  }

  public QbicNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
