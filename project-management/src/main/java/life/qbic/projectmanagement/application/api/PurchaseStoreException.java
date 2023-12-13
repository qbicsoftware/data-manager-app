package life.qbic.projectmanagement.application.api;

import java.io.Serial;

/**
 * <b>Purchase Store Exception</b>
 * <p>
 * Exception that shall be thrown if a
 * {@link life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase} item cannot be
 * stored in the persistence storage implementation.
 *
 * @since 1.0.0
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
