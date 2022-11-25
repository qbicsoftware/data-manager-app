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
    this.add(this.inputComponent);
    this.setValue(inputComponent.getEmptyValue());
    addListeners();
    setPresentationValue(generateModelValue());
    inputComponent.blur();
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
        this.setModelValue(it.getSource().getValue(), it.isFromClient());
        switchToDisplayComponent();
      } else {
        this.setErrorMessage(inputComponent.getErrorMessage());
      }
    });
    this.addValueChangeListener(it -> {
      if (!isInvalid()) {
        setPresentationValue(generateModelValue());
      }
    });
    this.getElement().addEventListener("click", e -> switchToInputComponent());
  }

  private void removeCustomFieldStyles() {
    this.getStyle().set("--lumo-text-field-size", "0");
  }

  @Override
  protected U generateModelValue() {
    return this.getValue();
  }

  @Override
  protected void setPresentationValue(U u) {
    S updatedDisplayComponent;
    //If the component value was set from the outside then that should be propagated to the edit field if reasonable value was provided.
    if (Objects.nonNull(u) && !isInvalid()) {
      inputComponent.setValue(u);
    } else {
      inputComponent.setValue(inputComponent.getEmptyValue());
    }
    //Empty value is dependent on input component ("" in TextField contrary to null for object selection)
    if (!Objects.equals(inputComponent.getEmptyValue(), u)) {
      updatedDisplayComponent = displayProvider.apply(u);
    } else {
      updatedDisplayComponent = emptyDisplayComponent;
    }
    this.add(updatedDisplayComponent);
    if (Objects.nonNull(displayComponent)) {
      boolean componentVisible = displayComponent.isVisible();
      this.remove(displayComponent);
      updatedDisplayComponent.setVisible(componentVisible);
    }
    add(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }

  public T getInputComponent() {
    return inputComponent;
  }
}
