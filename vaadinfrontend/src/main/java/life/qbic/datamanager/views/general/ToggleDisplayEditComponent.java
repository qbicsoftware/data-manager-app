package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.*;
import com.vaadin.flow.dom.Element;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @param <S> the component to display when in viewing mode
 * @param <T> the component to use for editing
 * @param <U> the value of the editing component
 * @since <version tag>
 */
@Tag("div")
public class ToggleDisplayEditComponent<S extends Component,
    T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & HasValidation & Focusable<T>,
    U> extends AbstractField<ToggleDisplayEditComponent<S, T, U>, U> implements HasSize {

  private static final Logger log = getLogger(ToggleDisplayEditComponent.class);

  private final Function<U, S> displayProvider;
  private final T inputComponent;
  private S displayComponent;
  private final S emptyDisplayComponent;

  /**
   * @param displayProvider       A function returning a display component given a value.
   * @param inputComponent        The component used to edit the value of this field.
   * @param emptyDisplayComponent The display component to show when the field is empty.
   */
  public ToggleDisplayEditComponent(Function<U, S> displayProvider,
                                    T inputComponent,
                                    S emptyDisplayComponent) {
    super(inputComponent.getEmptyValue());
    requireNonNull(displayProvider, "Display provider must not be null");
    requireNonNull(inputComponent, "Input component must not be null");
    requireNonNull(emptyDisplayComponent, "empty display component must not be null");
    this.displayProvider = displayProvider;
    this.inputComponent = inputComponent;
    this.emptyDisplayComponent = emptyDisplayComponent;
    this.displayComponent = emptyDisplayComponent;
    // attach input component as child to this element
    add(this.inputComponent);
    add(this.displayComponent);
    // initially update representation with input value
    setPresentationValue(this.inputComponent.getValue());
    setValue(this.inputComponent.getValue());

    switchToDisplay();
    registerClientExitActions();
    registerClientEnterActions();
  }

  /**
   * Returns the component used to edit the field value
   *
   * @return the component used to edit the field value
   */
  public T getInputComponent() {
    return inputComponent;
  }

  private void registerClientEnterActions() {
    // client enter is defined as a click on this component
    this.getElement().addEventListener("click", it -> {
      switchToEdit();
      inputComponent.focus();
    });
  }

  private void registerClientExitActions() {
    // client exit is defined as a blur event on the input element
    inputComponent.addBlurListener(it -> {
      updateValue(it.isFromClient());
      if (!inputComponent.isInvalid()) {
        switchToDisplay();
      }
    });
  }

  private void switchToDisplay() {
    inputComponent.setVisible(false);
    displayComponent.setVisible(true);
  }

  private void switchToEdit() {
    displayComponent.setVisible(false);
    inputComponent.setVisible(true);
  }

  private void updateValue(boolean fromClient) {
    setModelValue(generateModelValue(), fromClient);
    setPresentationValue(getValue());
  }

  @Override
  public void setValue(U value) {
    super.setValue(value);
    this.inputComponent.setValue(value);
  }

  /**
   * Generates the model value. If there is a valid input, returns the valid input. Otherwise:
   * returns the last valid value.
   *
   * @return the current input value if valid, the last valid value otherwise.
   */
  private U generateModelValue() {
    if (inputComponent.isInvalid()) {
      return this.getValue();
    }
    if (inputComponent.isEmpty()) {
      return this.getEmptyValue();
    }
    return inputComponent.getValue();
  }

  @Override
  protected void setPresentationValue(U u) {
    S updatedDisplayComponent = generateUpdatedDisplayComponent(u);
    replaceDisplayComponent(updatedDisplayComponent);
  }

  /**
   * Generates a new display component for the provided value. If the value equals the empty value
   * or is null, returns the emptyDisplayComponent.
   *
   * @param newValue the value for which to generate a display component
   * @return the generated display component, or the emptyDisplayComponent if no value was provided
   */
  private S generateUpdatedDisplayComponent(U newValue) {
    S updatedDisplayComponent;
    //Empty value is dependent on input component ("") in TextField contrary to null for object selection)
    if (!Objects.equals(getEmptyValue(), newValue)) {
      updatedDisplayComponent = displayProvider.apply(newValue);
    } else {
      updatedDisplayComponent = emptyDisplayComponent;
    }
    return updatedDisplayComponent;
  }

  private void replaceDisplayComponent(S updatedDisplayComponent) {
    if (Objects.isNull(displayComponent)) {
      throw new RuntimeException("Display component is null. This is not expected.");
    }
    boolean componentVisible = displayComponent.isVisible();
    updatedDisplayComponent.setVisible(componentVisible);
    remove(displayComponent);
    add(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }

  private void add(Component component) {
    Objects.requireNonNull(component, "Component to add cannot be null");
    getElement().appendChild(component.getElement());
  }

  private void remove(Component component) {
    Element parent = component.getElement().getParent();
    if (parent == null) {
      log.debug("Remove of a component with no parent does nothing.");
      return;
    }
    if (getElement().equals(parent)) {
      getElement().removeChild(component.getElement());
    } else {
      throw new IllegalArgumentException(
          "The given component (" + component + ") is not a child of this component");
    }
  }
}
