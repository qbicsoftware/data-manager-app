package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import static java.util.Objects.requireNonNull;

/**
 * Mixin interface for components that provide a binder for validation. Sets properties for invalid
 * state and error message string to show when invalid.
 */
public interface HasBinderValidation<T> extends HasValidationProperties {

  /**
   * Returns a binder used for validation. Must not be null!
   * <p>
   * This binder is used for validation.
   *
   * @return the binder used for validation
   */
  Binder<T> getBinder();

  /**
   * The default error message if all of the failing validators provide a blank error messages. Can
   * be empty, or null.
   *
   * @return the default error message
   */
  default String getDefaultErrorMessage() {
    return null;
  }

  /**
   * Validates based on the binder returned by {@link #getBinder()}. Updates the validation status
   * and sets an appropriate error message.
   * <p>
   * By default, the first failing validator's error message is set as the error message. If the
   * error message is blank, the next non-blank error message is set.
   * <p>
   * If all error messages are blank or null, {@link #getDefaultErrorMessage()} is used to set the
   * error message.
   *
   * @return this instance.
   */
  default HasBinderValidation<T> validate() {
    requireNonNull(getBinder(), "getBinder() must not be null");
    BinderValidationStatus<T> validationStatus = getBinder().validate();
    setInvalid(validationStatus.hasErrors());
    if (useBinderErrorMessage()) {
      setErrorMessage(validationStatus
          .getValidationErrors().stream()
          .filter(it -> !it.getErrorMessage().isBlank())
          .findFirst()
          .map(ValidationResult::getErrorMessage)
          .orElse(getDefaultErrorMessage()));
    } else {
      setErrorMessage(getDefaultErrorMessage());
    }
    return this;
  }

  /**
   * True if the binder error message should be used. If false, then the default error message is
   * used always.
   *
   * @return true if the binder error message is used; false if the default error message is used
   * only.
   */
  default boolean useBinderErrorMessage() {
    return true;
  }

  /**
   * Missing method from HasValidationProperties. Indiates whether this is marked as invalid.
   * <p>
   * Does not perform validation.
   *
   * @return true if no invalidation marker exists, false otherwise
   */
  default boolean isValid() {
    return !isInvalid();
  }

}
