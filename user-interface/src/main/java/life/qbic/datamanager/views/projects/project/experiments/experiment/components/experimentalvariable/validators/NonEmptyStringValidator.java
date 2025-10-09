package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators;

import static java.util.Objects.nonNull;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.validator.AbstractValidator;

public class NonEmptyStringValidator extends AbstractValidator<String> {

  /**
   * Constructs a validator with the given error message. The substring "{0}" is replaced by the
   * value that failed validation.
   *
   * @param errorMessage the message to be included in a failed result, not null
   */
  public NonEmptyStringValidator(String errorMessage) {
    super(errorMessage);
  }

  @Override
  public ValidationResult apply(String value, ValueContext context) {
    return toResult(value, isValid(value));
  }

  /**
   * Returns whether the given value is empty.
   *
   * @param value the value to validate
   * @return true if the value is valid, false otherwise
   */
  protected boolean isValid(String value) {
    return nonNull(value) && !value.isBlank();
  }
}
