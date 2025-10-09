package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators;

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;

public class ValidationSupport<T> {

  public interface SupportsValueChangeListener<V> {

    Registration addValueChangeListener(Consumer<ValueChangeEvent<V>> listener);

    interface ValueChangeEvent<V> {

      V getOldValue();

      V getValue();
    }
  }

  public interface HasValue<V> {

    V getValue();
  }

  public interface WorksWithValidationSupport<V> extends SupportsValueChangeListener<V>,
      HasValue<V> {

  }

  public enum ValidationMode {
    ON_VALUE_CHANGE,
    ON_DEMAND
  }

  private final WorksWithValidationSupport<T> field;
  private final List<Validator<T>> validators;
  private final Consumer<List<ValidationResult>> doOnValidation;
  private ValidationMode validationMode = ValidationMode.ON_DEMAND;

  private ValidationSupport(WorksWithValidationSupport<T> field, List<Validator<T>> validators,
      Consumer<List<ValidationResult>> doOnValidation) {
    this.field = Objects.requireNonNull(field);
    this.validators = Objects.requireNonNull(validators);
    this.doOnValidation = doOnValidation;
    field.addValueChangeListener(
        event -> {
          if (validationMode == ValidationMode.ON_VALUE_CHANGE) {
            doOnValidation.accept(runValidation(event.getValue()));
          }
        });
  }

  /**
   *
   * @param field          the field for which the validations should be applied
   * @param doOnValidation the action to perform on validation
   * @param <V>            the type of value the field supports
   * @return a validation support configured for this field
   */
  public static <V> ValidationSupport<V> forField(WorksWithValidationSupport<V> field,
      Consumer<List<ValidationResult>> doOnValidation) {
    Objects.requireNonNull(field);
    Objects.requireNonNull(doOnValidation);
    return new ValidationSupport<>(field, new ArrayList<>(), doOnValidation);
  }

  public ValidationSupport<T> addValidator(Validator<T> validator) {
    validators.add(Objects.requireNonNull(validator));
    return this;
  }

  private List<ValidationResult> runValidation(T value) {
    return validators.stream()
        .map(validator -> validator.apply(value, new ValueContext()))
        .toList();
  }

  public void validate() {
    doOnValidation.accept(runValidation(field.getValue()));
  }

  public void setValidationMode(@NonNull ValidationMode validationMode) {
    this.validationMode = Objects.requireNonNull(validationMode);
  }
}
