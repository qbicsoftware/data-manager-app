package life.qbic.projectmanagement.application.api;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class PurchaseStoreException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -6632058816119289144L;

  public PurchaseStoreException() {
  }

  public PurchaseStoreException(String message) {
    super(message);
  }

  public PurchaseStoreException(String message, Throwable cause) {
    super(message, cause);
  }

  public PurchaseStoreException(Throwable cause) {
    super(cause);
  }

  public PurchaseStoreException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
