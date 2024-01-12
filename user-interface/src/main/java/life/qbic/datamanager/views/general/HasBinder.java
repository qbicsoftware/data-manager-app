package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface HasBinder<T> extends HasValidationProperties {

  Binder<T> getBinder();

  default String defaultErrorMessage() {
    return "";
  }

  default HasBinder<T> validate() {
    BinderValidationStatus<T> validationStatus = getBinder().validate();
    setInvalid(validationStatus.hasErrors());
    setErrorMessage(validationStatus
        .getValidationErrors().stream()
        .filter(it -> !it.getErrorMessage().isBlank())
        .findFirst()
        .map(ValidationResult::getErrorMessage)
        .orElse(defaultErrorMessage()));
    return this;
  }

}
