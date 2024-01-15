package life.qbic.datamanager.views.general;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;

/**
 * Mixin interface for components that provide a binder for validation.
 * Sets properties for invalid state and error message string to show when invalid.
 */
public interface HasBinderValidation<T> extends HasValidationProperties {

  /**
   * Specifies the binder used for validation. Must not be null!
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
   * and sets an appropriate error message. By default, the first failing validator's error message
   * is set as the error message. If the error message is blank, the next non-blank error message is
   * set. If all error messages are blank or null, {@link #getDefaultErrorMessage()} is used to set
   * the error message.
   *
   * @return this instance.
   */
  default HasBinderValidation<T> validate() {
    requireNonNull(getBinder(), "getBinder() must not be null");
    BinderValidationStatus<T> validationStatus = getBinder().validate();
    setInvalid(validationStatus.hasErrors());
    setErrorMessage(validationStatus
        .getValidationErrors().stream()
        .filter(it -> !it.getErrorMessage().isBlank())
        .findFirst()
        .map(ValidationResult::getErrorMessage)
        .orElse(getDefaultErrorMessage()));
    return this;
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
