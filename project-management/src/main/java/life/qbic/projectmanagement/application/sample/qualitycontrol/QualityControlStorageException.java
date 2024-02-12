package life.qbic.projectmanagement.application.sample.qualitycontrol;

import java.io.Serial;
import life.qbic.projectmanagement.domain.model.sample.qualitycontrol.QualityControl;

/**
 * <b>QualityControlStorageException</b>
 * <p>
 * Exception that shall be thrown if a {@link QualityControl} item cannot be stored in the
 * persistence storage implementation.
 *
 * @since 1.0.0
 */
public class QualityControlStorageException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -3283512159754153169L;

  public QualityControlStorageException() {
  }

  public QualityControlStorageException(String message) {
    super(message);
  }

  public QualityControlStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public QualityControlStorageException(Throwable cause) {
    super(cause);
  }

  public QualityControlStorageException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
