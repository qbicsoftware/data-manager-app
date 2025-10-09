package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.function.Consumer;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators.ValidationSupport.SupportsValueChangeListener;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators.ValidationSupport.WorksWithValidationSupport;

public class ValidatableTextField extends TextField implements WorksWithValidationSupport<String>,
    ExposesValidStatus {

  private final ValidationSupport<String> validationSupport = ValidationSupport.forField(this,
      this::handleValidation);

  public ValidatableTextField() {
    super.setManualValidation(true);
  }

  @Override
  public Registration addValueChangeListener(
      Consumer<SupportsValueChangeListener.ValueChangeEvent<String>> listener) {
    return super.addValueChangeListener(event -> listener.accept(
        new SupportsValueChangeListener.ValueChangeEvent<String>() {
          @Override
          public String getOldValue() {
            return event.getOldValue();
          }

          @Override
          public String getValue() {
            return event.getValue();
          }
        }));
  }

  public void addValidator(Validator<String> validator) {
    validationSupport.addValidator(validator);
  }

  private void handleValidation(List<ValidationResult> validationResults) {
    if (validationResults.stream().noneMatch(ValidationResult::isError)) {
      setInvalid(false);
      setErrorMessage(null);
      return;
    }

    String joinedErrorMessage = validationResults.stream()
        .filter(ValidationResult::isError)
        .map(ValidationResult::getErrorMessage)
        .distinct()
        .reduce("", (s1, s2) -> s1 + "\n" + s2);
    setErrorMessage(joinedErrorMessage);
    setInvalid(true);
  }

  /**
   * Do not call this method. Use {@link #refreshValidation()} instead.
   */
  @Override
  protected void validate() {
    super.validate();
  }

  /**
   * Cannot call this method validate as the textfield does some bonkers automatic calling to the
   * validate method.
   */
  public void refreshValidation() {
    super.validate();
    validationSupport.validate();
  }

  @Override
  public boolean isValid() {
    return !isInvalid();
  }

  @Override
  public boolean isInvalid() {
    return super.isInvalid();
  }
}
