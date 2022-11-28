package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import java.util.Objects;
import java.util.function.Function;

/**
 * @param <S> the component to display when in viewing mode
 * @param <T> the component to use for editing
 * @param <U> the value of the editing component
 * @since <version tag>
 */
public class ToggleDisplayEditComponent<S extends Component, T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & HasValidation & Focusable<T>, U> extends
    CustomField<U> {

  private final Function<U, S> displayProvider;
  private final T inputComponent;
  private S displayComponent;
  private final S emptyDisplayComponent;

  public ToggleDisplayEditComponent(T inputComponent, Function<U, S> displayProvider,
      S emptyDisplayComponent) {
    this.displayComponent = emptyDisplayComponent;
    this.emptyDisplayComponent = emptyDisplayComponent;
    this.displayProvider = displayProvider;
    this.inputComponent = inputComponent;
    add(inputComponent);
    //init toggleComponent value for external listeners
    updateValue();
    addListeners();
    setPresentationValue(generateModelValue());
    switchToDisplayComponent();
    //Removes the space between elements in the contact element
    removeCustomFieldStyles();
  }

  private void switchToDisplayComponent() {
    displayComponent.setVisible(true);
    inputComponent.setVisible(false);
  }

  private void switchToInputComponent() {
    displayComponent.setVisible(false);
    inputComponent.setVisible(true);
    inputComponent.focus();
  }

  private void addListeners() {
    inputComponent.addBlurListener(it -> {
      if (!inputComponent.isInvalid()) {
        switchToDisplayComponent();
      }
      updateValue();
    });
    //ToDo Overwrite ValueChangeListener to check if original value in edit field differs from final value to account for combobox value change
    this.addValueChangeListener(it -> {
      if (!isInvalid() && !it.isFromClient()) {
        inputComponent.setValue(getValue());
      }
      setPresentationValue(getValue());
    });
    this.getElement().addEventListener("click", e -> switchToInputComponent());
  }

  private void removeCustomFieldStyles() {
    this.getStyle().set("--lumo-text-field-size", "0");
  }

  @Override
  protected U generateModelValue() {
    if (inputComponent.isInvalid()) {
      this.setErrorMessage(inputComponent.getErrorMessage());
      this.setInvalid(true);
    } else {
      setModelValue(inputComponent.getValue(), true);
    }
    return this.getValue();
  }

  @Override
  protected void setPresentationValue(U u) {
    S updatedDisplayComponent = generateUpdatedDisplayComponent(u);
    replaceDisplayComponent(updatedDisplayComponent);
  }

  private S generateUpdatedDisplayComponent(U newValue) {
    S updatedDisplayComponent;
    //Empty value is dependent on input component ("") in TextField contrary to null for object selection)
    if (!Objects.equals(inputComponent.getEmptyValue(), newValue)) {
      updatedDisplayComponent = displayProvider.apply(newValue);
    } else {
      updatedDisplayComponent = emptyDisplayComponent;
    }
    return updatedDisplayComponent;
  }

  private void replaceDisplayComponent(S updatedDisplayComponent) {
    if (Objects.nonNull(displayComponent)) {
      boolean componentVisible = displayComponent.isVisible();
      updatedDisplayComponent.setVisible(componentVisible);
      this.remove(displayComponent);
    }
    add(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }

  public T getInputComponent() {
    return inputComponent;
  }
}
